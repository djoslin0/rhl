package a2;

import Networking.PacketJoin;
import Networking.UDPClient;
import Networking.UDPServer;
import a2.GameEntities.*;
import a2.GameEntities.Box;
import com.jogamp.opengl.util.gl2.GLUT;
import myGameEngine.GameEntities.ShaderSkyBox;
import myGameEngine.GameEntities.StaticSkyBox;
import myGameEngine.GameEntities.Terrain;
import myGameEngine.GameEntities.WorldAxes;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.*;
import ray.input.GenericInputManager;
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

public class MyGame extends VariableFrameRateGame {

    //Networking arguments
    private String args[];

    private GenericInputManager im;
    private Camera camera;
    private Player player;
    private GoalSize goalSize;
    private int ai = 2;
    private AIPlayer aip;
    private HudText fpsText = new HudText(-80, -30, Color.white, GLUT.BITMAP_8_BY_13);

    public static boolean playMode = true;
    
    public static void main(String[] args) throws IOException {
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
        if (args.length > 0) {
            if (args[0].equals("s")) {
                UDPServer.createServer(8800);
                player = new Player((byte)0, true, (byte)0, Settings.get().spawnPoint.add(0, 0, -10));
                return;
            } else if (args[0].equals("c")) {
                UDPClient.createClient(InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
                UDPClient.send(new PacketJoin());

                while (player == null) {
                    player = UDPClient.getPlayer(UDPClient.getPlayerId());
                    UDPClient.update();
                }
                return;
            }
        }

        System.out.println("continuing without networking");
        player = new Player((byte)0, true, (byte)0, Settings.get().spawnPoint);
        new Player((byte)1, false, (byte)0, Settings.get().spawnPoint.add(10, 0, 0));
        new Player((byte)2, false, (byte)1, Settings.get().spawnPoint.add(20, 0, 0));
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
       //new ShaderSkyBox(engine, sm, this);
        new Goal(0);
        new Goal(1);
        new StaticSkyBox(sm.getRootSceneNode(),camera);
        new WorldAxes();
        new Ground();
        //aip = new AIPlayer((byte)200,false,(byte)0,Vector3f.createFrom(50f,0f,0f));
        //goalSize = GoalSize.GetGoalSize();
        if (!playMode) {
        	new Terrain();
        }

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

        if (!playMode) {
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
        } else {
	        // setup walls
        	float rinkWidth = 80;
        	float rinkLength = 130;
	        for (int i = 0; i < 8; i++) {
	            float scale = 32;
	            new Box(Vector3f.createFrom((i - 4) * scale, 0f, rinkWidth), scale);
	            new Box(Vector3f.createFrom((i - 4) * scale, 0f, -rinkWidth), scale);
	            new Box(Vector3f.createFrom(rinkLength, 0f, (i - 4) * scale), scale);
	            new Box(Vector3f.createFrom(-rinkLength, 0f, (i - 4) * -scale), scale);
	            
	        }
        }

        GL4RenderSystem rs = (GL4RenderSystem) engine.getRenderSystem();
        rs.addHud(fpsText);

        setupNetworking();
        setupInputs();
    }

    private void setupInputs() {
        im = new GenericInputManager();
        InputSetup.setupKeyboard(im, player);
        InputSetup.setupMouse(im, player);
        //InputSetup.listenToControllers(im, player, healthText);
    }

    @Override
    protected void update(Engine engine) {
        //goalSize.setSize();
        //goalSize.setScale();
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

