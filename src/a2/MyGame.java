package a2;

import Networking.UDPClient;
import Networking.UDPServer;
import a2.GameEntities.*;
import com.jogamp.opengl.util.gl2.GLUT;
import myGameEngine.GameEntities.ShaderSkyBox;
import myGameEngine.GameEntities.Terrain;
import myGameEngine.GameEntities.WorldAxes;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.EngineManager;
import myGameEngine.Singletons.PhysicsManager;
import myGameEngine.Singletons.TimeManager;
import myGameEngine.Singletons.UpdateManager;
import ray.input.GenericInputManager;
import ray.networking.IGameConnection;
import ray.rage.Engine;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.scene.Camera;
import ray.rage.scene.Light;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.controllers.RotationController;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

public class MyGame extends VariableFrameRateGame {

    //Networking arguments
    private String args[];

    private GenericInputManager im;
    private Camera camera;
    private Player player;

    private HudText score1Text = new HudText(15, -30, Color.BLUE, GLUT.BITMAP_HELVETICA_18);
    private HudText score2Text = new HudText(15, 15, Color.RED, GLUT.BITMAP_HELVETICA_18);
    private HudText fpsText = new HudText(-80, -30, Color.white, GLUT.BITMAP_8_BY_13);

    public static void main(String[] args) throws IOException {
        System.out.println(args.length);
        MyGame game = new MyGame(args);
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            game.shutdown();
            game.exit();
        }
    }

    public MyGame(String[] args) {
        super();
        this.args = args;
    }

    private void setupNetworking() throws IOException {
        if(args.length > 0) {
            if(args[0].equals("s")) {
                UDPServer.createServer(8800, IGameConnection.ProtocolType.UDP);
            }else if(args[0].equals("c")) {
                UDPClient.getClient(InetAddress.getByName(args[1]), Integer.parseInt(args[2]), IGameConnection.ProtocolType.UDP,camera);
                UDPClient.getClient().requestJoin();
            }
        }
    }

    @Override
    protected void setupWindowViewports(RenderWindow rw) {
        rw.addKeyListener(this);
        EngineManager.setRenderWindow(rw);
    }

    @Override
    protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
        rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
    }

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        camera = sm.createCamera("Camera1", Camera.Frustum.Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(camera);
    }

    @Override
    protected void setupScene(Engine engine, SceneManager sm) throws IOException {
        EngineManager.init(engine);
        PhysicsManager.initPhysics();
        new ShaderSkyBox(engine, sm, this);
        new WorldAxes();
        new Ground();
        new Terrain();

        // set up lights
        sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));

        Light dlight = sm.createLight("testLamp1", Light.Type.DIRECTIONAL);
        dlight.setAmbient(new Color(0, 0, 0));
        dlight.setDiffuse(new Color(.5f, .5f, 1f));
        dlight.setSpecular(new Color(0.5f, 0.4f, 1f));

        SceneNode rootNode = sm.getRootSceneNode();
        SceneNode dlightNode = rootNode.createChildSceneNode("dlightNode");
        dlightNode.attachObject(dlight);
        dlightNode.setLocalPosition(1f, 1f, 1f);


        new Puck(Vector3f.createFrom(15, 15, 15));

        // setup asteroid field
        Random rnd = new Random();
        SceneNode asteroidGroupNode = rootNode.createChildSceneNode("Asteroids");
        Vector3 rotationAxis = Vector3f.createFrom(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).normalize();
        RotationController rc = new RotationController(rotationAxis, 0.005f);
        rc.addNode(asteroidGroupNode);
        sm.addController(rc);
        for (int i = 0; i < 8; i++) {
            Vector3 pLocation = Vector3f.createFrom(rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f, rnd.nextFloat() - 0.5f).normalize();
            pLocation = pLocation.mult(40f + 40f * rnd.nextFloat());
            new Asteroid(asteroidGroupNode, pLocation, 1f, 1f);
        }

        // setup initial prizes
        for (int i = 0; i < 8; i++) {
            Vector3 pLocation = Vector3f.createFrom(0f, i * 10f, 0f);
            new Prize(pLocation);
        }

        // setup initial boxes
        for (int i = 0; i < 32; i++) {
            float scale = 3;
            Vector3 pLocation = Vector3f.createFrom(0f, i / 2.7f - 4f, i);
            new Box(pLocation, scale);
        }

        GL4RenderSystem rs = (GL4RenderSystem) engine.getRenderSystem();
        rs.addHud(score1Text);
        rs.addHud(score2Text);
        rs.addHud(fpsText);
        score2Text.text = "Score: 0";
        score1Text.text = "Press START on gamepad to begin.";

        setupNetworking();
        try {
            createPlayer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setupInputs();
    }
    private void createPlayer() throws IOException, InterruptedException {
        if(UDPClient.getClient()!=null) {
            while(player == null) {
                System.out.println("waiting");
                player = UDPClient.getClient().getPlayer();
                UDPClient.getClient().processPackets();
            }
            System.out.println("connected to Server");
        } else {
            System.out.println("continuing without networking");
            player = new Player(camera, Vector3f.createFrom(0f, 0f, 60f), 40000);
        }
    }

    private void setupInputs() {
        im = new GenericInputManager();
        InputSetup.setupKeyboard(im, player);
        InputSetup.setupMouse(im, player);
        InputSetup.listenToControllers(im, player, score1Text);
    }

    public void processNetworking(float elapseTime) throws IOException {
        if(UDPClient.getClient()!=null) {
            UDPClient.getClient().processPackets();
            UDPClient.getClient().requestPlayers();
            UDPClient.getClient().sendPositionInfo(player);
        }
    }
    
    @Override
    protected void update(Engine engine) {
        float delta = engine.getElapsedTimeMillis();
        if(UDPClient.getClient() != null){
            try {
                processNetworking(delta);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        TimeManager.update(delta);
        PhysicsManager.getWorld().stepSimulation(delta / 1000f, 10, 1f / 144f);
        im.update(delta);
        UpdateManager.update(delta);
        fpsText.text = "FPS: " + TimeManager.getFps();


    }
}

