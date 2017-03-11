/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 * A simple game animation panel.
 */
public class DvAnimatedPanel extends javax.swing.JPanel {
	/**
	 * The frame-rate controller task, this guides the animation.
	 */
	class FrameRateController extends TimerTask {
		private final Timer timer = new Timer("Animation");
		private final Runnable work = () -> {
			// This causes the paintComponent() method to be invoked.
			repaint(getBounds());
		};
		/**
		 * CFreates a new frame rate controller task.
		 */
		FrameRateController() {
			super();
		}
		/**
		 * Starts the controller task.
		 * @param delay Optional delay before the animation begins.
		 * @param period The period between frames.
		 * @return This object.
		 */
		private FrameRateController start(long delay, long period) {
			timer.schedule(this, delay, period);
			return this;
		}
		/**
		 * Stops the controller task. After this the task must be disposed, and
		 * cannot be reused, i.e. restarted.
		 */
		private void stop() {
			timer.cancel();
			cancel();
		}
		
		@Override
		public void run() {
			// Do the work using the EDT, basically repaint the panel, and everything
			// in it.
			SwingUtilities.invokeLater(work);
		}
	}

	private final FrameRateController frameRateController = new FrameRateController();
	private Image background;
	/**
	 * All the characters in this panel. Characters may be added and removed at any time.
	 */
	private final Map<String, DvCharacter> characterRepository = new HashMap<>(1024);
	/**
	 * All other components (non-character) in this panel. Components may be added and removed at any time.
	 */
	private final Map<String, DvComponent> componentRepository = new HashMap<>(128);
	private final AffineTransform affineXform = new AffineTransform(1.0, 0.0, 0.0, 1.0, 0, 0);
	/**
	 * Creates a new animated panel instance.
	 */
	@SuppressWarnings("OverridableMethodCallInConstructor")
	public DvAnimatedPanel() {
		initComponents();
		setFocusable(true);
	}

	/**
	 * Starts the animation. Must be invoked if you want to see anything moving.
	 * @param period The period, between frames.
	 * @return This panel.
	 */
	public DvAnimatedPanel start(long period) {
		frameRateController.start(0, period);
		return this;
	}
	
	/**
	 * Stops the animation.
	 */
	public void stop() {
		frameRateController.stop();
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics); 
		Rectangle bounds = getBounds();
		Graphics local = graphics.create();
		try {
			if(local instanceof Graphics2D) {
				Graphics2D g2d = (Graphics2D)local;
				if(background != null) {
					double scaleX = (double)bounds.width / (double)background.getWidth(null);
					double scaleY = (double)bounds.height / (double)background.getHeight(null);
					g2d.drawImage(background, new AffineTransform(scaleX, 0.0, 0.0, scaleY, 0.0, 0.0), null);
				}
				characterRepository.values().stream().forEach((character) -> {
					double scale = character.getScale();
					Point pt = character.getPoint();
					affineXform.setToScale(scale, scale);
					affineXform.setToTranslation(pt.x, pt.y);
					g2d.drawImage(character.getNextImage(), affineXform, null);
				});
				componentRepository.values().stream().forEach((comp) -> {
					comp.draw(g2d);
				});
			}
		} finally {
			local.dispose();
		}
	}

	/**
	 * Adds a character to the panel. Must be invoked from the EDT.
	 * @param character The new character.
	 */
	public void addCharacter(DvCharacter character) {
		characterRepository.put(character.name, character);
	}
	/**
	 * Adds a new component to the panel. All components must have unique names.
	 * Must be invoked from the EDT.
	 * @param component The new component.
	 */
	public void addComponent(DvComponent component) {
		componentRepository.put(component.getName(), component);
	}
	/**
	 * Removes a component from the panel. Must be invoked from the EDT.
	 * @param name The name of the target component.
	 */
	public void removeComponent(String name) {
		componentRepository.remove(name);
	}
	/**
	 * Removes a character from the panel. Must be invoked from the EDT.
	 * @param charName Name of the target character.
	 */
	public void removeCharacter(String charName) {
		characterRepository.remove(charName);
	}
	/**
	 * Sets the background image for this panel. Must be invoked from the EDT.
	 * @param bg The background image.
	 */
	public void setBackground(Image bg) {
		this.background = bg;
		repaint();
	}
	
	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 663, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 381, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
