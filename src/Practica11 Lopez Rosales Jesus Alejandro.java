import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;
import java.awt.*;
import java.awt.event.*;

class Objetos3D extends JFrame {
    private TransformGroup transformGroupCube;
    private TransformGroup transformGroupSphere;
    private Canvas3D canvas;
    private Timer rotationTimer;
    private double rotSpeedX = 0.02;
    private double rotSpeedY = 0.02;
    private double bounceHeight = 1;
    private double bounceSpeed = 0.05;
    private double bouncePosition = 0;

    public Objetos3D() {
        configurarVentana();
        inicializarEscena();
        iniciarAnimacion();
    }

    private void configurarVentana() {
        setTitle("Objetos 3D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());
    }

    private void inicializarEscena() {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);

        Screen3D screen = canvas.getScreen3D();
        screen.setPhysicalScreenWidth(0.0254 / 90.0 * screen.getSize().width);
        screen.setPhysicalScreenHeight(0.0254 / 90.0 * screen.getSize().height);

        getContentPane().add(canvas, BorderLayout.CENTER);

        SimpleUniverse universe = new SimpleUniverse(canvas);
        BranchGroup scene = createSceneGraph();

        View view = universe.getViewer().getView();
        view.setSceneAntialiasingEnable(true);
        view.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        universe.getViewingPlatform().setNominalViewingTransform();
        universe.addBranchGraph(scene);

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                ajustarVelocidadRotacion(e.getX(), e.getY());
            }
        });
    }

    private BranchGroup createSceneGraph() {
        BranchGroup root = new BranchGroup();

        setupLighting(root);
        setBackgroundImage(root, "src/img/background.jpg");

        transformGroupCube = new TransformGroup();
        transformGroupCube.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box cube = new Box(0.4f, 0.4f, 0.4f, createChromeMaterial(
                new Color3f(0.1f, 0.3f, 0.9f)
        ));

        Transform3D cubePosition = new Transform3D();
        cubePosition.setTranslation(new Vector3f(-0.8f, 0.0f, -2.0f));
        transformGroupCube.setTransform(cubePosition);
        transformGroupCube.addChild(cube);
        root.addChild(transformGroupCube);

        transformGroupSphere = new TransformGroup();
        transformGroupSphere.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Cylinder cylinder = new Cylinder(0.2f, 0.6f, Cylinder.GENERATE_NORMALS | Cylinder.GENERATE_TEXTURE_COORDS,
                createChromeMaterial(new Color3f(0.9f, 0.1f, 0.6f))
        );

        Transform3D cylinderPosition = new Transform3D();
        cylinderPosition.setTranslation(new Vector3f(0.8f, 0.0f, -2.0f));
        transformGroupSphere.setTransform(cylinderPosition);
        transformGroupSphere.addChild(cylinder);
        root.addChild(transformGroupSphere);

        root.compile();
        return root;
    }


    private void setupLighting(BranchGroup root) {
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);

        Color3f lightColor1 = new Color3f(1.0f, 1.0f, 1.0f);
        Vector3f lightDirection1 = new Vector3f(-1.0f, -1.0f, -1.0f);
        DirectionalLight light1 = new DirectionalLight(lightColor1, lightDirection1);
        light1.setInfluencingBounds(bounds);
        root.addChild(light1);

        Color3f lightColor2 = new Color3f(0.3f, 0.3f, 0.3f);
        Vector3f lightDirection2 = new Vector3f(1.0f, 1.0f, 1.0f);
        DirectionalLight light2 = new DirectionalLight(lightColor2, lightDirection2);
        light2.setInfluencingBounds(bounds);
        root.addChild(light2);

        Color3f ambientColor = new Color3f(0.2f, 0.2f, 0.2f);
        AmbientLight ambientLight = new AmbientLight(ambientColor);
        ambientLight.setInfluencingBounds(bounds);
        root.addChild(ambientLight);
    }

    private Appearance createChromeMaterial(Color3f baseColor) {
        Appearance appearance = new Appearance();

        Material material = new Material();
        material.setAmbientColor(new Color3f(0.2f, 0.2f, 0.2f));
        material.setDiffuseColor(baseColor);
        material.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
        material.setShininess(128.0f);
        material.setLightingEnable(true);

        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparencyMode(TransparencyAttributes.BLENDED);
        ta.setTransparency(0.2f);

        appearance.setMaterial(material);
        appearance.setTransparencyAttributes(ta);

        PolygonAttributes pa = new PolygonAttributes();
        pa.setCullFace(PolygonAttributes.CULL_BACK);
        pa.setBackFaceNormalFlip(true);
        appearance.setPolygonAttributes(pa);

        return appearance;
    }

    private void setBackgroundImage(BranchGroup root, String imagePath) {
        TextureLoader loader = new TextureLoader(imagePath, new Container());
        ImageComponent2D image = loader.getImage();

        if (image == null) {
            System.err.println("Error cargando la imagen de fondo: " + imagePath);
            return;
        }

        Background background = new Background();
        background.setImage(image);
        background.setImageScaleMode(Background.SCALE_FIT_ALL);
        background.setApplicationBounds(new BoundingSphere(new Point3d(0.0,0.0,0.0),1000));
        root.addChild(background);
    }

    private void iniciarAnimacion() {
        rotationTimer = new Timer(16, e -> {
            bouncePosition += bounceSpeed;
            double yOffset = Math.abs(Math.sin(bouncePosition)) * bounceHeight;

            Transform3D cubeTrans = new Transform3D();
            transformGroupCube.getTransform(cubeTrans);
            Vector3f cubePos = new Vector3f(-0.8f, (float) yOffset - 0.5f, -2.0f);
            cubeTrans.setTranslation(cubePos);
            aplicarRotacion(cubeTrans, rotSpeedX, rotSpeedY);
            transformGroupCube.setTransform(cubeTrans);

            Transform3D sphereTrans = new Transform3D();
            transformGroupSphere.getTransform(sphereTrans);
            Vector3f spherePos = new Vector3f(0.8f, (float) yOffset - 0.5f, -2.0f);
            sphereTrans.setTranslation(spherePos);
            aplicarRotacion(sphereTrans, -rotSpeedX, -rotSpeedY);
            transformGroupSphere.setTransform(sphereTrans);
        });
        rotationTimer.start();
    }

    private void aplicarRotacion(Transform3D transform, double deltaX, double deltaY) {
        Transform3D rotX = new Transform3D();
        Transform3D rotY = new Transform3D();
        rotX.rotX(deltaX);
        rotY.rotY(deltaY);

        transform.mul(rotX);
        transform.mul(rotY);
    }

    private void ajustarVelocidadRotacion(int mouseX, int mouseY) {
        int width = getWidth();
        int height = getHeight();
        rotSpeedX = ((double) mouseY / height - 0.5) * 0.1;
        rotSpeedY = ((double) mouseX / width - 0.5) * 0.1;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Objetos3D().setVisible(true));
    }
}