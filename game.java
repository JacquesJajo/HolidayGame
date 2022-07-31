import javax.swing.JFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.util.Random;
import java.util.ArrayList;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.Rectangle;
import java.util.HashMap;

class Bitmaps {
	static int w = 0xffffff;
	static int b = 0x00ffff;

	static int fb = 0x0000ff;
	static int ob = 0x000096;
	static int tr = 0xff00ff;

	public static int[] test = {
		b,b,b,b,b,b,b,b,
		b,b,w,b,b,w,b,b,
		b,b,w,b,b,w,b,b,
		b,b,w,b,b,w,b,b,
		b,b,w,b,b,w,b,b,
		b,b,b,b,b,b,b,b,
		b,b,b,b,b,b,b,b,
		b,b,b,b,b,b,b,b,	
	};

	public static int[] ufo = {
		tr,tr,tr,tr,tr,tr,tr,tr,
		tr,tr,tr,ob,ob,tr,tr,tr,
		tr,tr,ob,tr,tr,ob,tr,tr,
		tr,ob,tr,tr,tr,tr,ob,tr,
		ob,fb,fb,fb,fb,fb,fb,ob,
		tr,ob,ob,fb,fb,ob,ob,tr,
		tr,tr,tr,ob,ob,tr,tr,tr,
		tr,tr,tr,tr,tr,tr,tr,tr,
	};
}

abstract class Entity {
	public float x, y;
	protected Game game;
	protected boolean active;

	public Entity(float x, float y, Game game) {
		this.x=x;
		this.y=y;
		this.game=game;
		active = true;
	}

	public abstract void update(float delta);
	public abstract void render();

	public Rectangle getBounds() {
		return new Rectangle((int)x,(int)y,8,8);
	}
}

class Bullet extends Entity {
	float dx, dy;
	float speed;

	public Bullet(float x, float y, float dx, float dy, float speed,
		       	Game game) {
		super(x, y, game);
		this.dx = dx;
		this.dy = dy;
		this.speed = speed;
	}

	public void update(float delta) {
		if (!active) return;
		x += dx * speed * delta;
		y += dy * speed * delta;

		if (x < 0 || y < 0 || x >= game.width || y >= game.height) {
			active = false;
		}
	}

	public void render() {
		if (!active) return;
		game.drawRect(0xff0000,(int)x-2,(int)y-2,4,4);
	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle((int)x-2,(int)y-2,4,4);
	}
	
}

class Player extends Entity {

	public ArrayList<Bullet> bullets = new ArrayList<Bullet>();
	
	long next_time = 0;
	long interval =  500;
	boolean canShoot = true;
	
	float speed = 3.0f;

	public Player(float x, float y, Game game) {
		super(x, y, game);
	}

	public void update(float delta) {

		if (System.currentTimeMillis() > next_time) {
			canShoot = true;
		}

		for(Bullet b : bullets) {
			b.update(delta);
		}

		if (canShoot && game.input.isKeyDown(KeyEvent.VK_K)) {
			bullets.add(new Bullet(
				x+8, y+4, 1, 0, 5, game));
			canShoot = false;
			next_time = System.currentTimeMillis() + interval;
		}

		if (game.input.isKeyDown(KeyEvent.VK_W) &&
				y > 0) {
			y -= speed * delta;
		}

		if (game.input.isKeyDown(KeyEvent.VK_S) &&
				y < game.height - 8) {
			y += speed * delta;
		}
	}

	public void render() {
		for(Bullet b : bullets) {
			b.render();
		}

		game.drawBitmap(Bitmaps.test, (int)x, (int)y, 8, 8);
	}
}

class UFO extends Entity {

	float dx, dy, speed;
	Player player;

	float init_y;

	public UFO(float x, float y, float dx, float dy,
			float speed, Player player, Game game) {
		super(x,y,game);
		this.dx = dx;
		this.dy = dy;
		this.speed = speed;
		this.player = player;

		init_y = y;
	}

	public void update(float delta) {
		if(!active) return;
		x += dx * speed * delta;
		//y += dy * speed * delta;

		y = init_y + (float)Math.sin(x * 0.1f) * 2.5f;

		if (x < -8 || x > game.width) {
			active = false;
		}

		for (Bullet b : player.bullets) {
			if (b.active && b.getBounds().intersects(getBounds())) {
				active = false;
				b.active = false;
				break;
			}
		}
	}

	public void render() {
		if (!active) return;
		game.drawBitmap(Bitmaps.ufo,(int)x,(int)y,8,8);
	}
}

class UFOSpawner extends Entity {

	ArrayList<UFO> ufos = new ArrayList<UFO>();
	Random random = new Random();
	Player player;

	long next_ufo = 0;
	long interval = 120;

	public UFOSpawner(Player player, Game game) {
		super(0,0,game);
		this.player = player;
	}

	public void update(float delta) {
		if (System.currentTimeMillis() > next_ufo) {
			float y = (float)random.nextInt(game.height-8);
			ufos.add(new UFO((float)game.width,y,
				-1.0f,0.0f,5.0f,player,game));
			next_ufo = System.currentTimeMillis() + interval;
		}

		for (UFO u : ufos) {
			u.update(delta);
		}
	}

	public void render() {
		for (UFO u : ufos) {
			u.render();
		}
	}
}

class Cloud {
	public float x, y, width, height;
	public boolean active = true;

	public Cloud(float x, float y, float width, float height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	void checkBounds() {
		active = x > -width;
	}
}

class Background {
	static final int sky = 0x9085d0;
	static final int cloud1 = 0xf4c4d4;
	static final int cloud2 = 0xea92ab;
	static final int cloud3 = 0xaf7fc2;
	static final int h1 = 0x61567d;
	static final int h2 = 0x8c76be;

	ArrayList<Cloud> clouds1 = new ArrayList<Cloud>();
	ArrayList<Cloud> clouds2 = new ArrayList<Cloud>();
	ArrayList<Cloud> clouds3 = new ArrayList<Cloud>();

	float hills1 = 0.0f;
	float hills2 = 0.0f;

	long interval = 600;
	long next = 0;

	Random random = new Random();

	public void update(float delta, int w, int h) {

		if (System.currentTimeMillis() > next) {
			int d = random.nextInt(3);	
			int y = random.nextInt(h-8);
			int w1 = 16 + random.nextInt(16);
			w1 = (int) ((float)w1 / (1.0f+d*0.25f));
			int h1 = (int)(8.0f / (1.0f+d*0.25f));

			switch(d) {
			case 0:
				clouds1.add(new Cloud(w, y, w1, h1));
				break;
			case 1:
				clouds2.add(new Cloud(w, y, w1, h1));
				break;
			case 2:
				clouds3.add(new Cloud(w, y, w1, h1));
				break;
			}

			next = System.currentTimeMillis() + interval;
		}

		for (Cloud c : clouds1){
			if (c.active) {
				c.x -= 3.5f * delta;
				c.checkBounds();
			}
		}
		for (Cloud c : clouds2){
			if (c.active) {
				c.x -= 2.5f * delta;
				c.checkBounds();
			}
		}
		for (Cloud c : clouds3){
			if (c.active) {
				c.x -= 1.5f * delta;
				c.checkBounds();
			}
		}

		hills1 += 1.25f * delta;
		hills2 += 2.75f * delta;
	}

	public void render(Game game, int w, int h) {
		game.drawRect(sky,0,0,w,h);

		game.drawFunction((n) -> ((float)Math.sin((n+hills1)*0.1f)
					* 10.0f), h1, 0, game.height/2,
					game.width, game.height/2);

		for (Cloud c : clouds3)
			if (c.active)
				game.drawRect(cloud3,(int)c.x,(int)c.y,
					(int)c.width,(int)c.height);
		for (Cloud c : clouds2)
			if (c.active)
				game.drawRect(cloud2,(int)c.x,(int)c.y,
					(int)c.width,(int)c.height);

		game.drawFunction((n) -> ((float)Math.sin((n+hills2)*0.075f)
					* 5.0f), h2, 0,
					(int)((float)game.height * 0.75f),
					game.width,
					(int)((float)game.height * 0.75f));

		for (Cloud c : clouds1)
			if (c.active)
				game.drawRect(cloud1,(int)c.x,(int)c.y,
					(int)c.width,(int)c.height);
	}
}

class Input implements KeyListener {
	
	boolean[] keys = new boolean[256];	

	public void keyPressed(KeyEvent e) {
		keys[e.getKeyCode()] = true;
	}

	public void keyReleased(KeyEvent e) {
		keys[e.getKeyCode()] = false;
	}

	public boolean isKeyDown(int keyCode) {
		return keys[keyCode];
	}

	public void keyTyped(KeyEvent e) {}
}

interface DrawableFunction {
	float func(float x);
}

abstract class State {

	protected StateMachine states;
	protected Game game;

	public State(StateMachine states, Game game) {
		this.states = states;
		this.game = game;
	}

	public abstract void init();
	public abstract void update(float delta);
	public abstract void render();
}

class StateMachine {
	HashMap<String, State> states = new HashMap<String, State>();
	State current;

	public void add(String name, State state) {
		states.put(name, state);
	}

	public State get(String name) {
		return states.get(name);
	}

	public void change(String name) {
		current = states.get(name);
	}
	
	public void init() {
		current.init();
	}

	public void update(float delta) {
		current.update(delta);
	}

	public void render() {
		current.render();
	}
}

class GameState extends State {

	Background bg;
	Player player;
	UFOSpawner ufos;

	public GameState(StateMachine states, Game game) {
		super(states, game);
	}

	public void init() {
		bg = new Background();
		player = new Player(8, game.height/2, game);
		ufos = new UFOSpawner(player, game);
	}

	public void update(float delta) {
		bg.update(delta, game.width, game.height);
		player.update(delta);
		ufos.update(delta);
	}

	public void render() {
		bg.render(game, game.width, game.height-8);
		player.render();
		ufos.render();
	}
}

class Game extends Canvas implements Runnable {
	JFrame frame;
	BufferedImage screen;
	public int width, height;
	int scale;
	Random rand;

	public Input input = new Input();

	boolean running;

	public Game(int width, int height, int scale) {
		this.width = width;
		this.height = height;
		this.scale = scale;
		screen = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		rand = new Random();
		cls();
	}

	void cls() {
		for(int y=0;y<height;y++){
			for(int x=0;x<width;x++){
				screen.setRGB(x,y,0);
			}
		}
	}


	public void drawBitmap(int[] bitmap, int x, int y, int w, int h) {
		for(int y1=y;y1<y+h;y1++){
			for(int x1=x;x1<x+w;x1++){
				int x2=x1-x;
				int y2=y1-y;
				if(x1>=0&&y1>=0&&x1<width&&y1<height
					&& bitmap[x2+y2*w] != 0xff00ff){
					screen.setRGB(x1,y1,
							bitmap[x2+y2*w]);
				}
			}
		}
	}

	public void drawRect(int colour, int x, int y, int w, int h) {
		for(int y1=y;y1<y+h;y1++){
			for(int x1=x;x1<x+w;x1++){
				if(x1>=0&&y1>=0&&x1<width&&y1<height){
					screen.setRGB(x1,y1,colour);
				}
			}
		}
	}

	public void drawFunction(DrawableFunction f, int colour, 
			int x, int y, int w, int h) {
		for(int x1=0;x1<w;x1++){
			int y1 = (int)f.func(x1);
		       	int x2 = x + x1;
			int y2 = y + y1;
			for (int y3=y2; y3<y+h; y3++) {	
				if(x2>=0 && y3>=0 && x2<width && y3<height){
					screen.setRGB(x2,y3,colour);
				}
			}

		}
	}

	public void run() {
		frame = new JFrame("game");
		frame.setSize(width*scale, height*scale);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setMinimumSize(new Dimension(width*scale,height*scale));
		setMaximumSize(new Dimension(width*scale,height*scale));
		setPreferredSize(new Dimension(width*scale,height*scale));
		frame.add(this);
		frame.addKeyListener(input);
		frame.pack();
		frame.setVisible(true);

		createBufferStrategy(2);
		BufferStrategy bs = getBufferStrategy();
		Graphics g = bs.getDrawGraphics();

		running = true;

		float x = 0.0f;
		float y = 0.0f;
		
		StateMachine states = new StateMachine();
		states.add("Game", new GameState(states, this));

		states.change("Game");
		states.init();

		long last_time = System.nanoTime();
		while (running) {
			long time = System.nanoTime();
			float delta = (float) (((double)time - last_time)
					/ 100000000.0d);
			last_time = time;

			x += 3.0f * delta;
			y = x;
			states.update(delta);

			g.clearRect(0,0,width,height);
			
			cls();

			states.render();

			g.drawImage(screen,0,0,width*scale,height*scale,null);
			cls();
			bs.show();

			if (input.isKeyDown(KeyEvent.VK_ESCAPE)) {
				running = false;
			}
		}
		g.dispose();
		frame.dispatchEvent(new WindowEvent(frame,
					WindowEvent.WINDOW_CLOSING));
		System.exit(0);
		
	}
}

class Main {
	public static void main(String[] args) {
		Game game = new Game(160, 90, 4);
		Thread t = new Thread(game);
		t.start();
	}
}
