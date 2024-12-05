import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SolarSystemSimulation
 * Simula un sistema solar en 3D con rotación y traslación de los planetas,
 * un Sol con rotación y destello, e iluminación dinámica.
 */
class SolarSystemSimulation extends JFrame {
    private SimpleUniverse universe; // Universo de Java 3D
    private BranchGroup rootGroup; // Grupo raíz para todos los objetos de la escena
    private List<PlanetaryBody> planets; // Lista de planetas
    private Timer animationTimer; // Timer para controlar la animación
    private double timeElapsed = 0; // Tiempo transcurrido en la animación
    private PointLight glowLight; // Luz dinámica para simular el destello del Sol

    /**
     * Datos de los planetas:
     * [Radio de órbita, Periodo orbital, Radio del planeta, Velocidad de rotación]
     */
    private static final class PlanetData {
        static final double[][] PLANET_DATA = {
                {0, 0, 0.3, 0.002}, // Sol
                {0.4, 0.1, 0.02, 0.1}, // Mercurio
                {0.7, 0.2, 0.05, 0.05}, // Venus
                {1, 0.3, 0.05, 0.03}, // Tierra
                {1.5, 0.4, 0.04, 0.02}, // Marte
                {2.5, 0.8, 0.2, 0.01}, // Júpiter
                {3.5, 1.2, 0.15, 0.01}, // Saturno
                {4.5, 1.6, 0.1, 0.01}, // Urano
                {5.5, 2.0, 0.1, 0.01} // Neptuno
        };
    }

    /**
     * Clase interna para representar un cuerpo planetario.
     * Incluye órbita, rotación y apariencia del planeta.
     */
    private class PlanetaryBody {
        TransformGroup orbitGroup; // Grupo de transformación para la órbita
        TransformGroup planetGroup; // Grupo de transformación para el planeta
        Transform3D orbitTransform; // Transformación de la órbita
        Transform3D rotationTransform; // Transformación de la rotación
        double orbitRadius; // Radio de la órbita
        double orbitPeriod; // Periodo orbital
        double planetRadius; // Radio del planeta
        double rotationSpeed; // Velocidad de rotación

        /**
         * Constructor de PlanetaryBody.
         * Inicializa la órbita, rotación y apariencia del planeta.
         */
        PlanetaryBody(int planetIndex) {
            double[] planetData = PlanetData.PLANET_DATA[planetIndex];
            orbitRadius = planetData[0];
            orbitPeriod = planetData[1];
            planetRadius = planetData[2];
            rotationSpeed = planetData[3];

            createPlanetNode(planetIndex);
        }

        /**
         * Crea y configura los nodos para el planeta y su órbita.
         */
        private void createPlanetNode(int planetIndex) {
            orbitGroup = new TransformGroup();
            orbitGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            planetGroup = new TransformGroup();
            planetGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            // Texturas de los planetas
            String[] texturePaths = {
                    "sun.jpg", "mercury.jpg", "venus.jpg", "earth.jpg",
                    "mars.jpg", "jupiter.jpg", "saturn.jpg", "uranus.jpg", "neptune.jpg"
            };

            Appearance appearance = createPlanetAppearance(texturePaths[planetIndex]);
            Sphere planet = new Sphere((float) planetRadius,
                    Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS,
                    appearance);

            planetGroup.addChild(planet); // Añadir el planeta al grupo
            orbitGroup.addChild(planetGroup); // Añadir el grupo de planetas al de órbitas
            rootGroup.addChild(orbitGroup); // Añadir todo al grupo raíz
        }

        /**
         * Crea la apariencia de un planeta con textura y material.
         */
        private Appearance createPlanetAppearance(String texturePath) {
            Appearance appearance = new Appearance();

            // Cargar la textura
            Container container = new Container();
            texturePath = "src/img/" + texturePath;
            TextureLoader loader = new TextureLoader(texturePath, container);
            Texture texture = loader.getTexture();
            if (texture != null) {
                appearance.setTexture(texture);
            }

            // Configurar el material
            Material material = new Material();
            material.setAmbientColor(new Color3f(0.3f, 0.3f, 0.3f));
            material.setDiffuseColor(new Color3f(0.9f, 0.9f, 0.9f));
            material.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
            material.setShininess(100.0f);
            material.setLightingEnable(true);
            appearance.setMaterial(material);

            return appearance;
        }

        /**
         * Actualiza la posición del planeta en su órbita y su rotación.
         */
        public void updatePosition(double time) {
            if (orbitPeriod == 0) { // Si no tiene órbita (Sol)
                if (rotationTransform == null) rotationTransform = new Transform3D();
                rotationTransform.rotY(rotationSpeed * time); // Rotar sobre su eje Y
                planetGroup.setTransform(rotationTransform); // Aplicar la rotación al grupo del Sol
                return;
            }

            // Actualización de posición en órbita (para planetas)
            double angle = (2 * Math.PI * time) / orbitPeriod;
            float x = (float) (orbitRadius * Math.cos(angle));
            float z = (float) (orbitRadius * Math.sin(angle));

            orbitTransform = new Transform3D();
            orbitTransform.setTranslation(new Vector3f(x, 0, z));
            orbitGroup.setTransform(orbitTransform);

            rotationTransform = new Transform3D();
            rotationTransform.rotY(rotationSpeed * time * 1000);
            planetGroup.setTransform(rotationTransform);
        }

    }

    /**
     * Constructor principal de la simulación.
     */
    public SolarSystemSimulation() {
        setupFrame();
        setupUniverse();
        createSolarSystem();
        setupLighting();
        startAnimation();
    }

    /**
     * Configura la ventana de la simulación.
     */
    private void setupFrame() {
        setTitle("Solar System Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 800);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Configura el universo de Java 3D.
     */
    private void setupUniverse() {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);
        getContentPane().add(canvas);

        universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();

        rootGroup = new BranchGroup();
        rootGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
    }

    /**
     * Crea el sistema solar y agrega planetas al grupo raíz.
     */
    private void createSolarSystem() {
        planets = new ArrayList<>();
        for (int i = 0; i < PlanetData.PLANET_DATA.length; i++) {
            planets.add(new PlanetaryBody(i));
        }

        // Fondo de la galaxia
        Container container = new Container();
        TextureLoader loader = new TextureLoader("src/img/galaxy_background.jpg", container);
        ImageComponent2D image = loader.getImage();
        if (image != null) {
            Background background = new Background(image);
            background.setApplicationBounds(new BoundingSphere(new Point3d(), 1000.0));
            rootGroup.addChild(background);
        }

        rootGroup.compile();
        universe.addBranchGraph(rootGroup);
    }

    /**
     * Configura la iluminación de la escena, incluyendo el destello del Sol.
     */
    private void setupLighting() {
        BoundingSphere bounds = new BoundingSphere(new Point3d(), 1000.0);

        // Luz ambiental
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.2f, 0.2f, 0.2f));
        ambientLight.setInfluencingBounds(bounds);

        // Luz principal del Sol (estática)
        PointLight sunLight = new PointLight(
                new Color3f(1.0f, 1.0f, 0.9f),
                new Point3f(0f, 0f, 0f),
                new Point3f(1f, 0.1f, 0.01f));
        sunLight.setInfluencingBounds(bounds);

        // Luz dinámica del destello del Sol (glowLight)
        glowLight = new PointLight(
                new Color3f(1.0f, 0.8f, 0.3f),
                new Point3f(0f, 0f, 0f),
                new Point3f(1f, 0.01f, 0.001f));
        glowLight.setCapability(PointLight.ALLOW_COLOR_WRITE); // Habilitar cambios de color dinámicos
        glowLight.setInfluencingBounds(bounds);

        // Agregar luces al grupo de iluminación
        BranchGroup lightGroup = new BranchGroup();
        lightGroup.addChild(ambientLight);
        lightGroup.addChild(sunLight);
        lightGroup.addChild(glowLight);

        lightGroup.compile();
        universe.addBranchGraph(lightGroup);
    }


    /**
     * Inicia la animación del sistema solar.
     */
    private void startAnimation() {
        animationTimer = new Timer(16, e -> {
            timeElapsed += 0.001;

            // Actualizar posición y rotación de planetas
            for (PlanetaryBody planet : planets) {
                planet.updatePosition(timeElapsed);
            }

            // Animación del destello del Sol
            float intensity = 0.5f + 0.5f * (float) Math.sin(timeElapsed * 5);
            glowLight.setColor(new Color3f(intensity, intensity * 0.8f, intensity * 0.6f));
        });
        animationTimer.start();
    }


    /**
     * Método principal para iniciar la simulación.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SolarSystemSimulation simulation = new SolarSystemSimulation();
            simulation.setVisible(true);
        });
    }
}
