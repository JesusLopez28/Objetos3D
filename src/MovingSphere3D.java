import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.Box;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import javax.media.j3d.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.Timer;
import javax.vecmath.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.sound.sampled.Clip;

public class MovingSphere3D extends JFrame implements KeyListener, MouseListener, Runnable {
    private TransformGroup sphereTransformGroup;
    private Transform3D sphereTransform;
    private float x = 0.0f;
    private final float z = 0.0f;
    private float y = 0.0f;
    private BranchGroup root;
    private final ArrayList<Asteroid> asteroids = new ArrayList<>();
    private final ArrayList<Coin> coins = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private Timer timer;
    private JLabel scoreLabel;
    private JLabel coinLabel;
    private int score = 0;
    private int coinCount = 0;
    private int highScore = 0;
    private final CardLayout cardLayout;
    private Canvas3D canvas;
    private Clip backgroundMusicClip;
    private boolean isGameRunning = false;

    public MovingSphere3D() {
        setTitle("Space Dodge");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        setLayout(cardLayout);

        createMenuPanel();
        createGamePanel();

        cardLayout.show(getContentPane(), "MENU");
        setResizable(false);
    }

    private void createMenuPanel() {
        JPanel menuPanel = new JPanel(new GridBagLayout());
        menuPanel.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel titleLabel = new JLabel("SPACE DODGE");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));

        JButton startButton = new JButton("START GAME");
        startButton.setBackground(Color.GREEN);
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.addActionListener(_ -> startGame());

        JTextArea rulesText = new JTextArea(
                """
                        Instrucciones:
                        1. Usa las teclas de flecha (o W, A, S, D)\s
                        para mover la esfera.
                        2. Evita los asteroides para ganar puntos.
                        3. Recolecta monedas para puntos extra.
                        4. Dispara proyectiles haciendo clic para\s
                        destruir asteroides destructibles.
                        5. El juego se vuelve más difícil con el\s
                        tiempo a medida que aparecen más asteroides más rápido.
                        6. El juego termina si colisionas con un asteroide."""
        );

        rulesText.setEditable(false);
        rulesText.setFont(new Font("Arial", Font.PLAIN, 16));
        rulesText.setBackground(Color.BLACK);
        rulesText.setForeground(Color.WHITE);
        rulesText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rulesText.setPreferredSize(new Dimension(500, 200));

        gbc.gridy = 0;
        menuPanel.add(titleLabel, gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(30, 0, 0, 0);
        menuPanel.add(rulesText, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(20, 0, 0, 0);
        menuPanel.add(startButton, gbc);

        add(menuPanel, "MENU");
    }

    private void createGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout());

        canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        gamePanel.add(canvas, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scoreLabel = new JLabel("Score: 0");
        coinLabel = new JLabel("Coins: 0");
        scoreLabel.setForeground(Color.WHITE);
        coinLabel.setForeground(Color.WHITE);
        infoPanel.setBackground(Color.BLACK);
        infoPanel.add(scoreLabel);
        infoPanel.add(coinLabel);

        gamePanel.add(infoPanel, BorderLayout.NORTH);
        add(gamePanel, "GAME");

        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.setFocusable(true);

        SimpleUniverse universe = new SimpleUniverse(canvas);
        universe.getViewingPlatform().setNominalViewingTransform();

        root = createSceneGraph();
        addBackground(root);
        root.compile();
        universe.addBranchGraph(root);

        timer = new Timer(100, _ -> updateGame());
        timer.start();
    }

    private void startGame() {
        // Detener cualquier juego en curso
        if (isGameRunning) {
            timer.stop();

            // Limpiar todos los objetos del escenario
            for (Asteroid asteroid : asteroids) {
                root.removeChild(asteroid.getBranchGroup());
            }
            for (Coin coin : coins) {
                root.removeChild(coin.getBranchGroup());
            }
            for (Projectile projectile : projectiles) {
                root.removeChild(projectile.getBranchGroup());
            }

            // Limpiar listas
            asteroids.clear();
            coins.clear();
            projectiles.clear();
        }

        // Reiniciar variables y el escenario
        playBackgroundMusic();
        x = 0.0f;
        y = 0.0f;

        // Resetear la posición de la nave
        sphereTransform.setTranslation(new Vector3f(x, y, z));
        sphereTransformGroup.setTransform(sphereTransform);

        // Mostrar pantalla de juego
        cardLayout.show(getContentPane(), "GAME");
        score = 0;
        coinCount = 0;
        updateScoreDisplay();
        canvas.requestFocus();

        // Reiniciar el timer y el hilo de generación
        timer.restart();
        isGameRunning = true;
        new Thread(this).start();
    }


    private BranchGroup createSceneGraph() {
        BranchGroup root = new BranchGroup();
        root.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        root.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

        sphereTransformGroup = new TransformGroup();
        sphereTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        sphereTransformGroup.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);

        sphereTransform = new Transform3D();
        sphereTransformGroup.setTransform(sphereTransform);

        // Cambiar a un cubo con textura
        Appearance shipAppearance = new Appearance();
        TextureLoader loader = new TextureLoader("src/img/metal.jpg", this);
        Texture texture = loader.getTexture();
        shipAppearance.setTexture(texture);

        // Usar un Box en lugar de una Sphere
        Box ship = new Box(0.1f, 0.1f, 0.1f, Primitive.GENERATE_TEXTURE_COORDS, shipAppearance);

        TransformGroup shipTG = new TransformGroup();
        shipTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        shipTG.addChild(ship);

        // Crear interpolador de rotación
        Transform3D axis = new Transform3D();
        axis.setIdentity(); // Rotar alrededor del eje Y
        Alpha rotationAlpha = new Alpha(-1, 4000); // Repetición infinita y duración de 4 segundos
        RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, shipTG, axis, 0.0f, (float) Math.PI);
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));

        shipTG.addChild(rotator);

        sphereTransformGroup.addChild(shipTG);
        root.addChild(sphereTransformGroup);

        addLighting(root);
        return root;
    }


    private void addBackground(BranchGroup root) {
        TextureLoader loader = new TextureLoader("src/img/galaxy_background.jpg", this);
        ImageComponent2D image = loader.getImage();
        Background background = new Background(image);
        background.setApplicationBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        root.addChild(background);
    }

    private void addLighting(BranchGroup root) {
        // Luz direccional más potente
        Color3f lightColor = new Color3f(1.0f, 1.0f, 1.0f);
        Vector3f lightDirection = new Vector3f(-1.0f, -1.0f, -1.0f);
        DirectionalLight light = new DirectionalLight(lightColor, lightDirection);
        light.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        root.addChild(light);

        // Luz ambiental más intensa
        AmbientLight ambientLight = new AmbientLight(new Color3f(0.8f, 0.8f, 0.8f));
        ambientLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        root.addChild(ambientLight);
    }

    private void updateGame() {
        updateAsteroids();
        updateCoins();
        updateProjectiles();
        checkCollisions();
    }

    private void updateAsteroids() {
        Iterator<Asteroid> iterator = asteroids.iterator();
        while (iterator.hasNext()) {
            Asteroid asteroid = iterator.next();
            asteroid.move(); // Asteroids now move faster based on difficulty
            if (asteroid.isOutOfBounds()) {
                iterator.remove();
                root.removeChild(asteroid.getBranchGroup());
                updateScore(100);
            }
        }
    }

    private void updateCoins() {
        Iterator<Coin> iterator = coins.iterator();
        while (iterator.hasNext()) {
            Coin coin = iterator.next();
            coin.move();
            if (coin.isOutOfBounds()) {
                iterator.remove();
                root.removeChild(coin.getBranchGroup());
            }
        }
    }

    private void updateProjectiles() {
        Iterator<Projectile> projIterator = projectiles.iterator();
        while (projIterator.hasNext()) {
            Projectile projectile = projIterator.next();
            projectile.move();

            if (projectile.isOutOfBounds()) {
                projIterator.remove();
                root.removeChild(projectile.getBranchGroup());
            }
        }
    }

    private void checkCollisions() {
        // Colisiones con asteroides
        for (Asteroid asteroid : asteroids) {
            if (asteroid.checkCollision(x, y, z)) {
                playSound("src/sound/explosion.wav");
                gameOver();
                return;
            }
        }

        // Colisiones con monedas
        Iterator<Coin> coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            Coin coin = coinIterator.next();
            if (coin.checkCollision(x, y, z)) {
                coinIterator.remove();
                root.removeChild(coin.getBranchGroup());
                coinCount++;
                updateScore(1000);

                playSound("src/sound/coin.wav");
            }
        }

        // Colisiones con proyectiles
        Iterator<Projectile> projIterator = projectiles.iterator();
        while (projIterator.hasNext()) {
            Projectile projectile = projIterator.next();
            Iterator<Asteroid> asteroidIterator = asteroids.iterator();
            while (asteroidIterator.hasNext()) {
                Asteroid asteroid = asteroidIterator.next();
                if (asteroid.isDestructible() && asteroid.checkCollision(projectile.getX(), projectile.getY(), projectile.getZ())) {
                    projIterator.remove();
                    asteroidIterator.remove();
                    root.removeChild(asteroid.getBranchGroup());
                    root.removeChild(projectile.getBranchGroup());
                    updateScore(500);

                    playSound("src/sound/explosion.wav");
                }
            }
        }


        updateScore(1);  // Puntos por tiempo

        if (coinCount >= 5) {
            coinCount -= 5;
            updateScore(5000);
        }

        updateScoreDisplay();
    }

    private void gameOver() {
        isGameRunning = false;

        if (backgroundMusicClip != null) {
            backgroundMusicClip.stop();
            backgroundMusicClip.close();
        }

        playSound("src/sound/gameover.wav");

        timer.stop();
        JOptionPane.showMessageDialog(this,
                "Game Over!\n" +
                        "Score: " + score + "\n" +
                        "High Score: " + highScore,
                "Space Dodge",
                JOptionPane.INFORMATION_MESSAGE
        );
        cardLayout.show(getContentPane(), "MENU");
    }

    private void updateScore(int points) {
        score += points;
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        scoreLabel.setText("Score: " + score);
        coinLabel.setText("Coins: " + coinCount);

        if (score > highScore) {
            highScore = score;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        float moveSpeed = 0.3f; // Incrementa velocidad
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                if (y < 0.9f) y += moveSpeed;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                if (y > -0.9f) y -= moveSpeed;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (x > -0.9f) x -= moveSpeed;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (x < 0.9f) x += moveSpeed;
                break;
        }
        sphereTransform.setTranslation(new Vector3f(x, y, z));
        sphereTransformGroup.setTransform(sphereTransform);
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Projectile projectile = new Projectile(x, y, z);
        projectiles.add(projectile);
        root.addChild(projectile.getBranchGroup());

        playSound("src/sound/shoot.wav");
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void run() {
        int difficultyLevel = 1;
        long startTime = System.currentTimeMillis();

        while (isGameRunning && timer.isRunning()) {
            try {
                Thread.sleep(Math.max(500 - (difficultyLevel * 50), 200));

                Asteroid asteroid = new Asteroid();
                asteroid.setSpeed(0.5f + difficultyLevel * 0.1f);
                asteroids.add(asteroid);
                root.addChild(asteroid.getBranchGroup());

                if (Math.random() < 0.3) {
                    Coin coin = new Coin();
                    coins.add(coin);
                    root.addChild(coin.getBranchGroup());
                }

                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                difficultyLevel = (int) (elapsedTime / 20) + 1;
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void playSound(String soundFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(soundFile));
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playBackgroundMusic() {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File("src/sound/background.wav"));
            backgroundMusicClip = AudioSystem.getClip();
            backgroundMusicClip.open(audioStream);
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusicClip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MovingSphere3D frame = new MovingSphere3D();
            frame.setVisible(true);
        });
    }

    static class Projectile {
        private final BranchGroup branchGroup;
        private final TransformGroup transformGroup;
        private final Transform3D transform;
        private final float x;
        private final float y;
        private float z;

        public Projectile(float startX, float startY, float startZ) {
            branchGroup = new BranchGroup();
            branchGroup.setCapability(BranchGroup.ALLOW_DETACH);

            transformGroup = new TransformGroup();
            transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            transform = new Transform3D();
            this.x = startX;
            this.y = startY;
            this.z = startZ;

            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);

            // Proyectil pequeño y rápido
            Sphere projectile = new Sphere(0.05f, Primitive.GENERATE_TEXTURE_COORDS, createProjectileAppearance());
            TransformGroup rotationTG = new TransformGroup();
            rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            rotationTG.addChild(projectile);

            // Agregar rotación infinita
            Transform3D rotationAxis = new Transform3D();
            rotationAxis.setIdentity(); // Rotación alrededor del eje Y
            Alpha rotationAlpha = new Alpha(-1, 1000); // Repetición infinita, duración de 1 segundo
            RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, rotationTG, rotationAxis, 0.0f, (float) Math.PI);
            rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));

            rotationTG.addChild(rotator);
            transformGroup.addChild(rotationTG);

            branchGroup.addChild(transformGroup);
        }

        private Appearance createProjectileAppearance() {
            // Apariencia con imagen de fuego
            Appearance appearance = new Appearance();
            TextureLoader loader = new TextureLoader("src/img/fire.jpg", null);
            Texture texture = loader.getTexture();
            appearance.setTexture(texture);
            return appearance;
        }

        public void move() {
            z -= 0.7f;  // Movimiento más rápido hacia atrás
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);
        }

        public boolean isOutOfBounds() {
            return z < -5.0f;
        }

        public BranchGroup getBranchGroup() {
            return branchGroup;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }
    }

    static class Coin {
        private final BranchGroup branchGroup;
        private TransformGroup transformGroup;
        private final Transform3D transform;
        private final float x;
        private final float y;
        private float z = -5.0f;

        public Coin() {
            branchGroup = new BranchGroup();
            branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
            transformGroup = new TransformGroup();
            transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            transform = new Transform3D();
            x = randomCoord();
            y = randomCoord();
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);

            Sphere coin = new Sphere(0.1f, Primitive.GENERATE_TEXTURE_COORDS, createCoinAppearance());
            TransformGroup rotationTG = new TransformGroup();
            rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            rotationTG.addChild(coin);

            // Agregar rotación infinita
            Transform3D rotationAxis = new Transform3D();
            rotationAxis.setIdentity(); // Rotar alrededor del eje Y
            Alpha rotationAlpha = new Alpha(-1, 1000); // Repetición infinita, duración de 1 segundo
            RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, rotationTG, rotationAxis, 0.0f, (float) Math.PI * 2);
            rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));

            rotationTG.addChild(rotator);
            transformGroup.addChild(rotationTG);
            branchGroup.addChild(transformGroup);
        }

        private Appearance createCoinAppearance() {
            Appearance appearance = new Appearance();
            TextureLoader loader = new TextureLoader("src/img/coin.jpg", null);
            Texture texture = loader.getTexture();
            appearance.setTexture(texture);
            return appearance;
        }

        private float randomCoord() {
            return (float) (Math.random() * 2 - 1);
        }

        public void move() {
            z += 0.3f;  // Movimiento más lento
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);
        }

        public boolean isOutOfBounds() {
            return z > 0.0f;
        }

        public boolean checkCollision(float sx, float sy, float sz) {
            double distance = Math.sqrt(Math.pow(sx - x, 2) + Math.pow(sy - y, 2) + Math.pow(sz - z, 2));
            return distance < 0.3f;
        }

        public BranchGroup getBranchGroup() {
            return branchGroup;
        }
    }

    static class Asteroid {
        private final BranchGroup branchGroup;
        private final TransformGroup transformGroup;
        private final Transform3D transform;
        private final float x;
        private final float y;
        private float z = -5.0f;
        private final boolean destructible;

        public Asteroid() {
            branchGroup = new BranchGroup();
            branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
            transformGroup = new TransformGroup();
            transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

            transform = new Transform3D();
            x = randomCoord();
            y = randomCoord();
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);

            // 50% de probabilidad de ser destructible
            destructible = Math.random() < 0.5;

            Appearance appearance = createAppearance(destructible ?
                    "src/img/asteroid.jpg" : "src/img/alien.jpg");
            Sphere asteroid = new Sphere(0.2f, Primitive.GENERATE_TEXTURE_COORDS, 50, appearance);
            TransformGroup rotationTG = new TransformGroup();
            rotationTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            rotationTG.addChild(asteroid);

            // Agregar rotación infinita
            Transform3D rotationAxis = new Transform3D();
            rotationAxis.setIdentity(); // Rotar alrededor del eje Y
            Alpha rotationAlpha = new Alpha(-1, 1000); // Repetición infinita, duración de 1 segundo
            RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, rotationTG, rotationAxis, 0.0f, (float) Math.PI);
            rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));

            rotationTG.addChild(rotator);
            transformGroup.addChild(rotationTG);
            branchGroup.addChild(transformGroup);
        }

        private float randomCoord() {
            return (float) (Math.random() * 2 - 1);
        }

        private Appearance createAppearance(String texturePath) {
            Appearance appearance = new Appearance();
            TextureLoader loader = new TextureLoader(texturePath, null);
            Texture texture = loader.getTexture();
            appearance.setTexture(texture);

            // Si no es destructible, añadir un color grisáceo
            if (!destructible) {
                ColoringAttributes coloringAttributes = new ColoringAttributes(
                        new Color3f(0.5f, 0.5f, 0.5f), ColoringAttributes.FASTEST
                );
                appearance.setColoringAttributes(coloringAttributes);
            }

            return appearance;
        }

        public void move() {
            z += 0.5f;
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);
        }

        public boolean checkCollision(float sx, float sy, float sz) {
            double distance = Math.sqrt(Math.pow(sx - x, 2) + Math.pow(sy - y, 2) + Math.pow(sz - z, 2));
            return distance < 0.3f;
        }

        public boolean isOutOfBounds() {
            if (z > 0.0f) {
                branchGroup.detach();
                return true;
            }
            return false;
        }

        public BranchGroup getBranchGroup() {
            return branchGroup;
        }

        public boolean isDestructible() {
            return destructible;
        }

        // SET SPEED
        public void setSpeed(float speed) {
            z = -5.0f;
            transform.setTranslation(new Vector3f(x, y, z));
            transformGroup.setTransform(transform);
        }
    }
}