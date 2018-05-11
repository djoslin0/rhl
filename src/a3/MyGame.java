package a3;

import Networking.PacketJoin;
import Networking.UDPClient;
import Networking.UDPServer;
import a3.GameEntities.*;
import a3.GameEntities.Box;
import com.jogamp.opengl.util.gl2.GLUT;
import myGameEngine.GameEntities.*;
import myGameEngine.Helpers.HudText;
import myGameEngine.Singletons.*;
import ray.input.GenericInputManager;
import ray.rage.Engine;
import ray.rage.game.Game;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.scene.*;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyGame extends VariableFrameRateGame {

    private GenericInputManager im;
    private Camera camera;
    private Player player;
    private GoalSize goalSize;
    private HudText fpsText = new HudText(-80, -30, Color.white, GLUT.BITMAP_8_BY_13);

    public static void main(String[] args) throws IOException {
        CommandLine.read(args);
        Settings.initScript();
        MyGame game = new MyGame();
        try {
            game.startup();
            game.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            PrintWriter writer = new PrintWriter("crashlog.txt", "UTF-8");
            e.printStackTrace(writer);
            writer.close();
        } finally {
            shutdown(game);
        }
    }

    private static void shutdown(Game game) {
        try {
            game.shutdown();
        } catch (Exception ex) {}
        game.exit();
    }

    private void spawnBots() {
        if (UDPClient.hasClient()) { return; }
        int botCount = UDPServer.hasServer() ? Settings.get().serverBotCount.intValue() : Settings.get().localBotCount.intValue();
        if (CommandLine.getBots() > -1) { botCount = CommandLine.getBots(); }
        for (int i = 0; i < botCount; i++) {
            byte id = (byte) (i + 2);
            Player.Team team = (i % 2) == 0 ? Player.Team.Blue : Player.Team.Orange;
            byte headId = (byte) (Math.random() * 2 + 1);
            AIPlayer bot = new AIPlayer(id, false, team, headId);
            if (UDPServer.hasServer()) { UDPServer.addPlayer(bot); }
        }
    }

    private void setupNetworking() throws IOException {
        if (CommandLine.isServer()) {
            UDPServer.createServer(CommandLine.getPort());
            player = new Player((byte)0, true, Player.Team.Orange, (byte)1);
            return;
        }

        if (CommandLine.isClient()) {
            UDPClient.createClient(CommandLine.getIp(), CommandLine.getPort());
            UDPClient.send(new PacketJoin(CommandLine.getHeadId()));

            while (player == null) {
                player = UDPClient.getPlayer(UDPClient.getPlayerId());
                UDPClient.update();
            }
            return;
        }

        System.out.println("continuing without networking");
        player = new Player((byte)1, true, Player.Team.Orange, (byte)1);

    }

    @Override
    protected void setupWindowViewports(RenderWindow rw) {
        rw.addKeyListener(this);
        EngineManager.setRenderWindow(rw);
    }

    @Override
    protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
        CommandLine.DisplaySettings ds = CommandLine.getDisplaySettings();
        if (ds != null) {
            rs.createRenderWindow(new DisplayMode(ds.width, ds.height, ds.bitDepth, ds.refreshRate), ds.fullscreen);
        } else {
            rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
        }
    }

    @Override
    protected void setupCameras(SceneManager sm, RenderWindow rw) {
        camera = sm.createCamera("MainCamera", Camera.Frustum.Projection.PERSPECTIVE);

        // set FOV
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        DisplayMode displayMode = g.getDefaultScreenDevice().getDisplayMode();

        float hfov = CommandLine.getFov();
        float hfovRad = hfov * (float)Math.PI / 180f;
        float vfovRad = (float)(2 * Math.atan(Math.tan(hfovRad/2) * displayMode.getHeight() / displayMode.getWidth()));
        int vfov = (int)Math.ceil(vfovRad * 180f / Math.PI);
        camera.getFrustum().setFieldOfViewY(Degreef.createFrom(vfov));

        rw.getViewport(0).setCamera(camera);
    }

    @Override
    protected void setupScene(Engine engine, SceneManager sm) throws IOException {
        EngineManager.init(engine);
        PhysicsManager.initPhysics();
        AudioManager.initialize();

        new StaticSkyBox(sm.getRootSceneNode(),camera);

        //new WorldAxes();

        new Terrain();
        new Rink();
        new Ground();

        new Goal(Player.Team.Orange);
        new Goal(Player.Team.Blue);

        //goalSize = GoalSize.GetGoalSize();

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

        GL4RenderSystem rs = (GL4RenderSystem) engine.getRenderSystem();
        rs.addHud(fpsText);

        setupNetworking();
        spawnBots();
        setupInputs();
    }

    private void setupInputs() {
        im = new GenericInputManager();
        InputSetup.setupKeyboard(im, player);
        InputSetup.setupMouse(im, player);
        initMouseMode();
        //InputSetup.listenToControllers(im, player, healthText);
    }

    private void initMouseMode() {
        // create the cursor
        Toolkit t = Toolkit.getDefaultToolkit();
        Image i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none");
        
        // hide the cursor
        Canvas canvas = EngineManager.getEngine().getRenderSystem().getCanvas();
        canvas.setCursor(noCursor);
    }

    private long nodeCount(Node node) {
        int count = node.getChildCount();
        for (Node n : node.getChildNodes()) {
           count += nodeCount(n);
        }
        return count;
    }

    private void debugCounts() {
        System.out.println("------------------------");
        if (UDPServer.hasServer()) {
            System.out.println("Players: " + UDPServer.getPlayers().size());
        } else if (UDPClient.hasClient()) {
            System.out.println("Players: " + UDPClient.getPlayers().size());
        }
        System.out.println("Entities: " +  EngineManager.getSceneManager().getEntities().spliterator().getExactSizeIfKnown());
        System.out.println("RigidBodies: " +  PhysicsManager.getRigidBodies().size());
        System.out.println("Physics Callbacks: " +  PhysicsManager.getCallbackCount());
        System.out.println("Registered Collisions: " +  PhysicsManager.getRegisteredCollisionCount());
        System.out.println("Updates: " +  UpdateManager.getUpdateCount());
        System.out.println("Nodes: " +  nodeCount(EngineManager.getSceneManager().getRootSceneNode()));
    }

    @Override
    protected void update(Engine engine) {
        //goalSize.update();
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

        //debugCounts();
    }
}

