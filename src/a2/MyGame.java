package a2;

import Networking.PacketJoin;
import Networking.UDPClient;
import Networking.UDPServer;
import a2.GameEntities.*;
import com.jogamp.opengl.util.gl2.GLUT;
import myGameEngine.GameEntities.ShaderSkyBox;
import myGameEngine.GameEntities.Terrain;
import myGameEngine.GameEntities.WorldAxes;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.*;
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
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Time;

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
                UDPServer.createServer(8800);
                player = new Player((byte)0, true, (byte)0, Settings.get().spawnPoint.add(0, 0, -10));
                return;
            }else if(args[0].equals("c")) {
                UDPClient.createClient(InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
                UDPClient.send(new PacketJoin());

                while(player == null) {
                    player = UDPClient.getPlayer(UDPClient.getPlayerId());
                    UDPClient.update();
                }
                return;
            }
        }

        System.out.println("continuing without networking");
        player = new Player((byte)0, true, (byte)0, Settings.get().spawnPoint);
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
        camera = sm.createCamera("MainCamera", Camera.Frustum.Projection.PERSPECTIVE);
        rw.getViewport(0).setCamera(camera);
    }

    @Override
    protected void setupScene(Engine engine, SceneManager sm) throws IOException {
        EngineManager.init(engine);
        PhysicsManager.initPhysics();
        Settings.initScript();
        new ShaderSkyBox(engine, sm, this);
        new WorldAxes();
        new Ground();
        new Terrain();

        // set up lights
        sm.getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));

        Light dlight = sm.createLight("testLamp1", Light.Type.DIRECTIONAL);
        dlight.setAmbient(Settings.get().ambientColor);
        dlight.setDiffuse(Settings.get().diffuseColor);
        dlight.setSpecular(Settings.get().specularColor);

        SceneNode rootNode = sm.getRootSceneNode();
        SceneNode dlightNode = rootNode.createChildSceneNode("dlightNode");
        dlightNode.attachObject(dlight);
        dlightNode.setLocalPosition(Settings.get().lightDirection);


        new Puck(Settings.get().puckSpawnPoint);

        // setup initial prizes
        for (int i = 0; i < 8; i++) {
            Vector3 pLocation = Vector3f.createFrom(0f, i * 10f, 50f);
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
        score2Text.text = "Here too!";
        score1Text.text = "Text goes here.";

        setupNetworking();
        setupInputs();
    }

    private void setupInputs() {
        im = new GenericInputManager();
        InputSetup.setupKeyboard(im, player);
        InputSetup.setupMouse(im, player);
        InputSetup.listenToControllers(im, player, score1Text);
    }

    @Override
    protected void update(Engine engine) {
        float delta = engine.getElapsedTimeMillis();
        if (UDPClient.hasClient()) {
            UDPClient.update();
        } else if (UDPServer.hasServer()) {
            UDPServer.update();
        }
        TimeManager.update(delta);
        im.update(delta);
        UpdateManager.update(delta);
        fpsText.text = "FPS: " + TimeManager.getFps();


    }
}

