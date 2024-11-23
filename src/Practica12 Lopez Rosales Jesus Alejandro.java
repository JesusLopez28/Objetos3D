import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;
import java.awt.*;
import java.awt.event.*;

class Iluminacion extends JFrame {
    private TransformGroup transformGroupCube;
    private TransformGroup transformGroupSphere;
    private TransformGroup transformGroupShadowCube;
    private TransformGroup transformGroupShadowSphere;
    private Canvas3D canvas;
    private Timer rotationTimer;
    private double rotSpeedX = 0.02;
    private double rotSpeedY = 0.02;
    private double bouncePosition = 0;
    private double bounceSpeed = 0.05;

    public Iluminacion() {
        configurarVentana();
        inicializarEscena();
        iniciarAnimacion();
    }

    private void configurarVentana() {
        setTitle("Iluminación");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());
    }

    private void inicializarEscena() {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);

        getContentPane().add(canvas, BorderLayout.CENTER);

        SimpleUniverse universe = new SimpleUniverse(canvas);
        BranchGroup scene = createSceneGraph();

        View view = universe.getViewer().getView();
        view.setSceneAntialiasingEnable(true);

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
        setBackgroundImage(root, "src/img/fondo.jpg");

        transformGroupCube = new TransformGroup();
        transformGroupCube.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box cube = new Box(0.4f, 0.4f, 0.4f, createChromeMaterial(new Color3f(0.1f, 0.1f, 0.9f)));

        Transform3D cubePosition = new Transform3D();
        cubePosition.setTranslation(new Vector3f(-1.0f, 0.0f, -2.0f));
        transformGroupCube.setTransform(cubePosition);
        transformGroupCube.addChild(cube);
        root.addChild(transformGroupCube);

        transformGroupSphere = new TransformGroup();
        transformGroupSphere.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Cylinder cylinder = new Cylinder(0.2f, 0.6f, Cylinder.GENERATE_NORMALS | Cylinder.GENERATE_TEXTURE_COORDS,
                createChromeMaterial(new Color3f(0.9f, 0.1f, 0.1f)));

        Transform3D cylinderPosition = new Transform3D();
        cylinderPosition.setTranslation(new Vector3f(1.0f, 0.0f, -2.0f));
        transformGroupSphere.setTransform(cylinderPosition);
        transformGroupSphere.addChild(cylinder);
        root.addChild(transformGroupSphere);

        agregarSombras(root);

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

        appearance.setTransparencyAttributes(ta);
        appearance.setMaterial(material);
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
        background.setApplicationBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 1000));
        root.addChild(background);
    }

    private void agregarSombras(BranchGroup root) {
        transformGroupShadowCube = new TransformGroup();
        transformGroupShadowCube.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box shadowCube = new Box(0.4f, 0.01f, 0.4f, createShadowAppearance());
        transformGroupShadowCube.addChild(shadowCube);
        root.addChild(transformGroupShadowCube);

        transformGroupShadowSphere = new TransformGroup();
        transformGroupShadowSphere.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        Box shadowSphere = new Box(0.4f, 0.01f, 0.4f, createShadowAppearance());
        transformGroupShadowSphere.addChild(shadowSphere);
        root.addChild(transformGroupShadowSphere);
    }

    private Appearance createShadowAppearance() {
        Appearance shadowAppearance = new Appearance();

        Color3f shadowColor = new Color3f(0.0f, 0.0f, 0.0f);
        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparencyMode(TransparencyAttributes.BLENDED);
        ta.setTransparency(0.5f);

        shadowAppearance.setTransparencyAttributes(ta);

        Material shadowMaterial = new Material(shadowColor, shadowColor, shadowColor, shadowColor, 1.0f);
        shadowMaterial.setLightingEnable(false);
        shadowAppearance.setMaterial(shadowMaterial);

        return shadowAppearance;
    }

    private void iniciarAnimacion() {
        rotationTimer = new Timer(16, e -> {
            bouncePosition += bounceSpeed;
            double xOffset = Math.sin(bouncePosition) * 0.5;

            Transform3D cubeTrans = new Transform3D();
            transformGroupCube.getTransform(cubeTrans);
            Vector3f cubePos = new Vector3f((float) xOffset - 0.7f, -0.2f, -2.0f);

            Transform3D sphereTrans = new Transform3D();
            transformGroupSphere.getTransform(sphereTrans);
            Vector3f spherePos = new Vector3f((float) -xOffset + 0.7f, -0.2f, -2.0f);

            cubeTrans.setTranslation(cubePos);
            aplicarRotacion(cubeTrans, rotSpeedX, rotSpeedY);
            transformGroupCube.setTransform(cubeTrans);

            sphereTrans.setTranslation(spherePos);
            aplicarRotacion(sphereTrans, -rotSpeedX, -rotSpeedY);
            transformGroupSphere.setTransform(sphereTrans);


            actualizarSombras(cubePos, spherePos);
        });
        rotationTimer.start();
    }

    private void actualizarSombras(Vector3f cubePos, Vector3f spherePos) {
        Transform3D shadowCubeTrans = new Transform3D();
        shadowCubeTrans.setTranslation(new Vector3f(cubePos.x, -1.0f, cubePos.z));
        transformGroupShadowCube.setTransform(shadowCubeTrans);

        Transform3D shadowSphereTrans = new Transform3D();
        shadowSphereTrans.setTranslation(new Vector3f(spherePos.x, -1.0f, spherePos.z));
        transformGroupShadowSphere.setTransform(shadowSphereTrans);
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
        SwingUtilities.invokeLater(() -> {
            Iluminacion ventana = new Iluminacion();
            ventana.setVisible(true);
        });
    }
}
