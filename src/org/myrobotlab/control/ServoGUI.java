/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.control;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.ServoController;
import org.slf4j.Logger;

/**
 * Servo GUIService - displays details of Servo state Lesson learned ! Servos to
 * properly function need to be attached to a controller This gui previously
 * sent messages to the controller. To simplify things its important to send
 * messages only to the bound Servo - and let it attach to the controller versus
 * sending messages directly to the controller. 1 display - 1 service - keep it
 * simple
 *
 */
public class ServoGUI extends ServiceGUI implements ActionListener {

	private class SliderListener implements ChangeListener {
		@Override
		public void stateChanged(javax.swing.event.ChangeEvent e) {

			boundPos.setText(String.format("%d", slider.getValue()));

			if (myService != null) {
				myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
			} else {
				log.error("can not send message myService is null");
			}
		}
	}

	public final static Logger log = LoggerFactory.getLogger(ServoGUI.class.getCanonicalName());

	static final long serialVersionUID = 1L;

	JLabel boundPos = null;
	JButton attachButton = new JButton("attach");

	JButton updateLimitsButton = new JButton("update limits");

	JSlider slider = new JSlider(0, 180, 90);
	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);

	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);
	JComboBox<String> controller = new JComboBox<String>();

	// JComboBox<Integer> pin = new JComboBox<Integer>();
	JComboBox<Integer> pinList = new JComboBox<Integer>();
	// DefaultComboBoxModel<String> controllerModel = new DefaultComboBoxModel<String>();

	DefaultComboBoxModel<Integer> pinModel = new DefaultComboBoxModel<Integer>();
	JTextField posMin = new JTextField("0");

	JTextField posMax = new JTextField("180");

	Servo myServo = null;

	SliderListener sliderListener = new SliderListener();

	public ServoGUI(final String boundServiceName, final GUIService myService, final JTabbedPane tabs) {
		super(boundServiceName, myService, tabs);
		myServo = (Servo) Runtime.getService(boundServiceName);

		pinModel.addElement(null);
		for (int i = 0; i < 54; i++) {
			pinModel.addElement(i);
		}
		// determine not worth querying the controller to its pin list
	}

	// GUIService's action processing section - data from user
	@Override
	public void actionPerformed(final ActionEvent event) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Object o = event.getSource();
				if (o == controller) {
					String controllerName = (String) controller.getSelectedItem();
					log.info(String.format("controller event %s", controllerName));
					if (controllerName != null && controllerName.length() > 0) {

						// NOT WORTH IT - JUST BUILD 48 PINS !!!
						// ServoController sc = (ServoController)
						// Runtime.getService(controllerName);

						// NOT WORTH THE TROUBLE !!!!
						// @SuppressWarnings("unchecked")
						// ArrayList<Pin> pinList = (ArrayList<Pin>)
						// myService.sendBlocking(controllerName, "getPinList");
						// log.info("{}", pinList.size());

						// FIXME - get Local services relative to the servo
						// pinModel.removeAllElements();
						// pinModel.addElement(null);

						// for (int i = 0; i < pinList.size(); ++i) {
						// pinModel.addElement(pinList.get(i).pin);
						// }

						// pin.invalidate();

					}
				}

				if (o == attachButton) {
					if (attachButton.getText().equals("attach")) {
						send("attach", controller.getSelectedItem(), (int) pinList.getSelectedItem());
					} else {
						send("detach", controller.getSelectedItem());
					}
					return;
				}

				if (o == updateLimitsButton) {
					send("setMinMax", Integer.parseInt(posMin.getText()), Integer.parseInt(posMax.getText()));
					return;
				}

				if (o == right) {
					slider.setValue(slider.getValue() + 1);
					return;
				}

				if (o == left) {
					slider.setValue(slider.getValue() - 1);
					return;
				}

			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Servo.class);
		// subscribe("controllerSet", "controllerSet");
		// subscribe("pinSet", "pinSet");
		// subscribe("attached", "attached");
		// subscribe("detached", "detached");
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Servo.class);
		// unsubscribe("controllerSet", "controllerSet");
		// unsubscribe("pinSet", "pinSet");
		// subscribe("attached", "attached");
		// subscribe("detached", "detached");
	}

	synchronized public void getState(final Servo servo) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				removeListeners();
				refreshControllers();

				ServoController sc = servo.getController();

				if (sc != null) {
					controller.setSelectedItem(sc.getName());

					Integer servoPin = servo.getPin();

					if (servoPin != null)
						pinList.setSelectedItem(servoPin);
				}

				if (servo.isControllerSet()) {
					attachButton.setText("detach");
					controller.setEnabled(false);
					pinList.setEnabled(false);
				} else {
					attachButton.setText("attach");
					controller.setEnabled(true);
					pinList.setEnabled(true);
				}

				if (servo.getPos() == null) {
					boundPos.setText("");
				} else {
					int pos = servo.getPos();
					boundPos.setText(Integer.toString(pos));
					slider.setValue(pos);
				}

				// In the inverted case, these are reversed
				int min = Math.min(servo.getMinInput().intValue(), servo.getMaxInput().intValue());
				int max = Math.max(servo.getMinInput().intValue(), servo.getMaxInput().intValue());
				slider.setMinimum(min);
				slider.setMaximum(max);

				posMin.setText(servo.getMin().toString());
				posMax.setText(servo.getMax().toString());

				restoreListeners();
			}
		});

	}

	@Override
	public void init() {

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		input.add(slider, gc);
		slider.addChangeListener(sliderListener);

		gc.gridwidth = 2;
		gc.gridx = 1;
		++gc.gridy;
		input.add(left, gc);
		++gc.gridx;

		input.add(right, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;

		JPanel control = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		control.add(attachButton, gc);
		++gc.gridx;

		control.add(controller, gc);

		++gc.gridx;
		control.add(new JLabel("pin"), gc);

		++gc.gridx;
		control.add(pinList, gc);

		display.add(control);
		display.add(input);

		gc.gridx = 0;
		++gc.gridy;

		JPanel limits = new JPanel();
		limits.add(updateLimitsButton);
		limits.add(new JLabel("min "));
		limits.add(posMin);
		limits.add(new JLabel(" max "));
		limits.add(posMax);

		limits.add(new JLabel(" "));
		boundPos = new JLabel("90");

		limits.add(boundPos);

		display.add(limits, gc);

		updateLimitsButton.addActionListener(this);
		left.addActionListener(this);
		right.addActionListener(this);
		controller.addActionListener(this);
		attachButton.addActionListener(this);
		pinList.addActionListener(this);

		// http://stackoverflow.com/questions/6205433/jcombobox-focus-and-mouse-click-events-not-working
		// jComboBox1.getEditor().getEditorComponent().addMouseListener(...);
		// have to add mouse listener to the MetalComboButton embedded in the
		// JComboBox
		//* No longer needed
		// Component[] comps = controller.getComponents();
		//for (int i = 0; i < comps.length; i++) {
			//comps[i].addMouseListener(this); // JComboBox composite listener -
			// have to get all the sub
			// components
			/*
			 * comps[i].addMouseListener(new MouseAdapter() { public void
			 * mouseClicked(MouseEvent me) { System.out.println("clicked"); }
			 * });
			 */
		// }

		// controller.getEditor().getEditorComponent().addMouseListener(this);
		pinList.setModel(pinModel);

		refreshControllers();
	}
	
	public void getPinList() {
		List<Integer> mbl = myServo.pinList;
		for (int i = 0; i < mbl.size(); i++) {
			pinList.addItem(mbl.get(i));
		}
	}


	// a controller has been set
	/*
	 * public void displayController(final ServoController sc, final ServoGUI
	 * mygui) { SwingUtilities.invokeLater(new Runnable() { public void run() {
	 * controller.removeActionListener(mygui); pinModel.removeAllElements(); //
	 * FIXME - get Local services relative to the servo
	 * pinModel.addElement(null);
	 * 
	 * ArrayList<Pin> pinList = sc.getPinList(); for (int i = 0; i <
	 * pinList.size(); ++i) { pinModel.addElement(pinList.get(i).pin); }
	 * 
	 * pin.invalidate(); } });
	 * 
	 * }
	 */

	public void refreshControllers() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				myServo.refreshControllers();
				controller.removeAllItems();
				List<String> c = myServo.controllers;	
				for (int i = 0; i < c.size(); ++i) {
					controller.addItem(c.get(i));
				}
				controller.setSelectedItem(myServo.controllerName);
			}
		});
	}

	public void removeListeners() {
		controller.removeActionListener(this);
		pinList.removeActionListener(this);
		slider.removeChangeListener(sliderListener);
	}

	public void restoreListeners() {
		controller.addActionListener(this);
		pinList.addActionListener(this);
		slider.addChangeListener(sliderListener);
	}

}