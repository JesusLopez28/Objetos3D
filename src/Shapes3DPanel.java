import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;

import java.awt.*;
import java.awt.event.*;

public class Shapes3DPanel extends JPanel {
    private TransformGroup cubeTransformGroup;
    private TransformGroup cylinderTransformGroup;
    private Timer rotationTimer;
    private double bouncePosition = 0.0;
    private double bounceSpeed = 0.05;
    private double bounceHeight = 0.5;
    private TransformGroup viewingTransformGroup;

    public Shapes3DPanel() {
        setLayout(new GridLayout(1, 2));  // Two columns for the two canvases
        setPreferredSize(new Dimension(800, 600));

        // Canvas for the cube
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas1 = new Canvas3D(config);
        add(canvas1);

        // Canvas for the cylinder
        Canvas3D canvas2 = new Canvas3D(config);
        add(canvas2);

        // Create a SimpleUniverse for each shape
        SimpleUniverse universe1 = new SimpleUniverse(canvas1);
        SimpleUniverse universe2 = new SimpleUniverse(canvas2);

        // Configure the view for each universe
        configureView(universe1);
        configureView(universe2);

        // Create the BranchGroups and add them to each universe
        BranchGroup scene1 = createCubeSceneGraph();
        BranchGroup scene2 = createCylinderSceneGraph();
        universe1.addBranchGraph(scene1);
        universe2.addBranchGraph(scene2);

        // Initialize and start the rotation timer
        rotationTimer = new Timer(16, e -> rotateShapes());
        rotationTimer.start();
    }

    private void configureView(SimpleUniverse universe) {
        ViewingPlatform viewingPlatform = universe.getViewingPlatform();
        viewingTransformGroup = viewingPlatform.getViewPlatformTransform();
        Transform3D viewTransform = new Transform3D();
        viewTransform.setTranslation(new Vector3d(0.0, 0.5, 12.0));  // Initial camera position
        Transform3D rotX = new Transform3D();
        rotX.rotX(-Math.PI / 16.0); // Slightly downwards
        viewTransform.mul(rotX);
        viewingTransformGroup.setTransform(viewTransform);
    }

    private BranchGroup createCubeSceneGraph() {
        BranchGroup root = new BranchGroup();

        // Configure the background
        TextureLoader loader = new TextureLoader("src/img/background.jpeg", this);
        Background background = new Background(loader.getImage());
        background.setApplicationBounds(new BoundingSphere(new Point3d(), 100.0));
        root.addChild(background);

        // Configure the lighting
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        DirectionalLight light1 = new DirectionalLight(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Vector3f(-1.0f, -1.0f, -1.0f)
        );
        light1.setInfluencingBounds(bounds);
        root.addChild(light1);

        // Create the cube
        cubeTransformGroup = new TransformGroup();
        cubeTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance cubeAppearance = new Appearance();
        Material cubeMaterial = new Material(
                new Color3f(1.0f, 0.0f, 0.0f),  // ambient
                new Color3f(0.0f, 0.0f, 0.0f),  // emissive
                new Color3f(1.0f, 0.0f, 0.0f),  // diffuse
                new Color3f(0.7f, 0.7f, 0.7f),  // specular
                128.0f                          // shininess
        );
        cubeAppearance.setMaterial(cubeMaterial);

        Box cube = new Box(2f, 2f, 2f, Box.GENERATE_NORMALS, cubeAppearance);

        // Position the cube
        Transform3D cubePosition = new Transform3D();
        cubePosition.setTranslation(new Vector3f(0.0f, 0.0f, 0.0f));
        cubeTransformGroup.setTransform(cubePosition);
        cubeTransformGroup.addChild(cube);
        root.addChild(cubeTransformGroup);

        root.compile();
        return root;
    }

    private BranchGroup createCylinderSceneGraph() {
        BranchGroup root = new BranchGroup();

        // Configure the background
        TextureLoader loader = new TextureLoader("src/img/background.jpeg", this);
        Background background = new Background(loader.getImage());
        background.setApplicationBounds(new BoundingSphere(new Point3d(), 100.0));
        root.addChild(background);

        // Configure the lighting
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        DirectionalLight light1 = new DirectionalLight(
                new Color3f(0.8f, 0.8f, 0.8f),
                new Vector3f(-1.0f, -1.0f, -1.0f)
        );
        light1.setInfluencingBounds(bounds);
        root.addChild(light1);

        // Create the cylinder
        cylinderTransformGroup = new TransformGroup();
        cylinderTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Appearance cylinderAppearance = new Appearance();
        Material cylinderMaterial = new Material(
                new Color3f(0.0f, 0.0f, 1.0f),  // ambient
                new Color3f(0.0f, 0.0f, 0.0f),  // emissive
                new Color3f(0.0f, 0.0f, 1.0f),  // diffuse
                new Color3f(0.7f, 0.7f, 0.7f),  // specular
                128.0f                          // shininess
        );
        cylinderAppearance.setMaterial(cylinderMaterial);

        Cylinder cylinder = new Cylinder(2f, 4.0f, Cylinder.GENERATE_NORMALS, cylinderAppearance);

        // Position the cylinder
        Transform3D cylinderPosition = new Transform3D();
        cylinderPosition.setTranslation(new Vector3f(0.0f, 0.0f, 0.0f));
        cylinderTransformGroup.setTransform(cylinderPosition);
        cylinderTransformGroup.addChild(cylinder);
        root.addChild(cylinderTransformGroup);

        root.compile();
        return root;
    }


    private void rotateShapes() {
        double rotationAngle = 0.01;

        // Update bounce position
        bouncePosition += bounceSpeed;
        double yOffset = Math.sin(bouncePosition) * bounceHeight;

        // Rotate and translate the cube
        Transform3D cubeTransform = new Transform3D();
        cubeTransformGroup.getTransform(cubeTransform);
        Transform3D cubeRotation = new Transform3D();
        cubeRotation.rotX(rotationAngle);
        cubeTransform.mul(cubeRotation);
        cubeRotation.rotY(rotationAngle);
        cubeTransform.mul(cubeRotation);
        cubeRotation.rotZ(rotationAngle);
        cubeTransform.mul(cubeRotation);

        Vector3f cubeTranslation = new Vector3f();
        cubeTransform.get(cubeTranslation);
        cubeTranslation.y = (float) yOffset;
        cubeTransform.setTranslation(cubeTranslation);
        cubeTransformGroup.setTransform(cubeTransform);

        // Rotate and translate the cylinder in the opposite direction
        Transform3D cylinderTransform = new Transform3D();
        cylinderTransformGroup.getTransform(cylinderTransform);
        Transform3D cylinderRotation = new Transform3D();
        cylinderRotation.rotX(-rotationAngle);
        cylinderTransform.mul(cylinderRotation);
        cylinderRotation.rotY(-rotationAngle);
        cylinderTransform.mul(cylinderRotation);
        cylinderRotation.rotZ(-rotationAngle);
        cylinderTransform.mul(cylinderRotation);

        Vector3f cylinderTranslation = new Vector3f();
        cylinderTransform.get(cylinderTranslation);
        cylinderTranslation.y = (float) yOffset;
        cylinderTransform.setTranslation(cylinderTranslation);
        cylinderTransformGroup.setTransform(cylinderTransform);

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java3D Shapes");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Shapes3DPanel panel = new Shapes3DPanel();
        frame.add(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}