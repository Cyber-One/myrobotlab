package org.myrobotlab.jme3;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.JMonkeyEngine;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.BorderLayout.Position;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.ConsumingMouseListener;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.BaseStyles;

public class MainMenuState extends BaseAppState {
  transient JMonkeyEngine jme = null;
  transient Jme3App app;
  transient Container main;
  transient Label breadCrumbs;
  transient Node guiNode;

  VersionedReference<TabbedPanel.Tab> selectionRef;
  private Label statusLabel;

  TabbedPanel tabs;

  Button parentButton;

  Label title;

  TextField x;
  TextField y;
  TextField z;

  TextField xRot;
  TextField yRot;
  TextField zRot;

  TextField search;
  Label scale;

  // Label children;

  Button update;
  Button searchButton;

  Container childrenContainer;
  // Map<String, Button> children = new TreeMap<String, Button>();
  List<Button> children = new ArrayList<Button>();

  final static Logger log = LoggerFactory.getLogger(JMonkeyEngine.class);

  /**
   * FYI - this is all initialized JMEMain thread ..
   */
  public MainMenuState(JMonkeyEngine jme) {
    this.jme = jme;
    app = jme.getApp();
    guiNode = app.getGuiNode();
    // Initialize the globals access so that the default
    // components can find what they need.
    GuiGlobals.initialize(app);
    // Load the 'glass' style
    BaseStyles.loadGlassStyle();
    // Set 'glass' as the default style when not specified
    GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
  }

  @SuppressWarnings("unchecked")
  protected void addInfoTab() {

    x = new TextField("0.000");
    y = new TextField("0.000");
    z = new TextField("0.000");

    xRot = new TextField("0.000");
    yRot = new TextField("0.000");
    zRot = new TextField("0.000");

    update = new Button("update");

    search = new TextField("");
    searchButton = new Button("search");
    childrenContainer = new Container();

    Container contents = new Container();

    Container sub = new Container();
    sub.addChild(new Label("x:"), 0, 0);
    sub.addChild(new Label("y:"), 0, 2);
    sub.addChild(new Label("z:"), 0, 4);
    sub.addChild(x, 0, 1);
    sub.addChild(y, 0, 3);
    sub.addChild(z, 0, 5);

    scale = new Label("");

    sub.addChild(new Label("xRot:"), 1, 0);
    sub.addChild(new Label("yRot:"), 1, 2);
    sub.addChild(new Label("zRot:"), 1, 4);
    sub.addChild(xRot, 1, 1);
    sub.addChild(yRot, 1, 3);
    sub.addChild(zRot, 1, 5);
    sub.addChild(new Label("scale:"), 2, 0);
    sub.addChild(scale, 2, 1);
    sub.addChild(update, 3, 5);
    contents.addChild(sub);

    statusLabel = contents.addChild(new Label("Status"));
    statusLabel.setInsets(new Insets3f(2, 5, 2, 5));

    // Add some actions that will manipulate the document model independently
    // of the text field
    Container buttons = contents.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
    buttons.setInsets(new Insets3f(5, 5, 5, 5));
    
    // close
    Button button = new Button("close");
    button.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        onDisable();
      }
    });
    buttons.addChild(button);
    
    // save
    button = new Button("save");
    button.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        jme.saveSpatial(jme.getSelected());
      }
    });
    buttons.addChild(button);
    
    // rename
    button = new Button("rename");
    button.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        // jme.rename(jme.getSelected());
      }
    });
    buttons.addChild(button);
    
    // lookat
    button = new Button("look at");
    button.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        jme.lookAt(jme.getSelected());
      }
    });
    buttons.addChild(button);
      
    
    buttons.addChild(new Button("hide"));
    
    buttons.addChild(new Button("clone"));
    // buttons.addChild(new Button("rotate"));
    buttons.addChild(new Button("bind"));

    // --------children--------------
    contents.addChild(search);
    contents.addChild(searchButton);
    contents.addChild(new Label("children"));
    contents.addChild(childrenContainer);
    // children = contents.addChild(new Label(""));
    // children.setMaxWidth(400);

    tabs.addTab("info", contents);
  }

  protected void addHelpTab() {

    Container contents = new Container();
    contents.addChild(new Label("Control Keys:"));
    contents.addChild(new Label("forward      ctrl + ↑"));
    contents.addChild(new Label("back         ctrl + ↓"));
    contents.addChild(new Label("pan up              ↑"));
    contents.addChild(new Label("pan down            ↓"));
    contents.addChild(new Label("pan left            ←"));
    contents.addChild(new Label("pan right           →"));
    contents.addChild(new Label("rotate left  ctrl + ←"));
    contents.addChild(new Label("rotate right ctrl + →"));
    contents.addChild(new Label("select root node  - R"));
    // label.setInsets(new Insets3f(5, 5, 5, 5));

    tabs.addTab("help", contents);
  }

  @Override
  protected void cleanup(Application app) {
    // TODO Auto-generated method stub

  }

  @SuppressWarnings("unchecked")
  @Override // part of Lemur "standard"
  protected void initialize(Application appx) {

    main = new Container();
    guiNode = app.getGuiNode();
    guiNode.attachChild(main);
    // main.setPreferredSize(new Vector3f(300, jme.getSettings().getWidth(),
    // 30));

    MouseEventControl.addListenersToSpatial(main, ConsumingMouseListener.INSTANCE);
    main.setLayout(new BorderLayout());
    main.setLocalTranslation(10, jme.getSettings().getHeight() / 2, 0);

    Container north = new Container();
    Container center = new Container();
    Container south = new Container();

    main.addChild(north, Position.North);
    main.addChild(center, Position.Center);
    main.addChild(south, Position.South);

    title = north.addChild(new Label("selected"));
    title.setFontSize(16);
    title.setInsets(new Insets3f(10, 10, 0, 10));

    DragHandler dragHandler = new DragHandler();
    dragHandler.setDraggableLocator(new Function<Spatial, Spatial>() {
      public Spatial apply(Spatial spatial) {
        return spatial.getParent();
      }
    });

    breadCrumbs = new Label("                                        ");
    north.addChild(breadCrumbs);

    parentButton = center.addChild(new Button("parent:"));
    // Button floor = center.addChild(new Button("floor"));

    parentButton.addClickCommands(new Command<Button>() {
      @Override
      public void execute(Button source) {
        System.out.println("nav mode");
        Spatial selected = jme.getSelected();
        if (selected != null) {
          jme.setSelected(selected.getParent());
        }
      }
    });

    CursorEventControl.addListenersToSpatial(north, dragHandler);

    tabs = south.addChild(new TabbedPanel());
    tabs.setInsets(new Insets3f(5, 5, 5, 5));
    selectionRef = tabs.getSelectionModel().createReference();

    addInfoTab();
    addHelpTab();

  }

  public void loadGui() {
    initialize(app);
  }

  @Override
  protected void onDisable() {
    main.removeFromParent();
  }

  @Override
  protected void onEnable() {
    guiNode.attachChild(main);
    GuiGlobals.getInstance().requestFocus(main);
  }

  @SuppressWarnings("unchecked")
  public void putText(Spatial spatial) {

    if (spatial == null) {
      log.error("putText spatial is null");
      return;
    }

    if (spatial != null) {
      Spatial p = spatial.getParent();
      if (p != null) {
        parentButton.setText("parent: " + p);
      } else {
        parentButton.setText("parent:");
      }
    }

    Vector3f xyz = spatial.getWorldTranslation();
    Quaternion q = spatial.getLocalRotation();
    float[] angles = new float[3]; // yaw, roll, pitch
    q.toAngles(angles);

    x.setText(String.format("%.3f", xyz.x));
    y.setText(String.format("%.3f", xyz.y));
    z.setText(String.format("%.3f", xyz.z));

    // 2012 and the javadoc is still wrong ?
    zRot.setText(String.format("%.3f", angles[0] * FastMath.RAD_TO_DEG));
    xRot.setText(String.format("%.3f", angles[1] * FastMath.RAD_TO_DEG));
    yRot.setText(String.format("%.3f", angles[2] * FastMath.RAD_TO_DEG));

    Vector3f sc = spatial.getLocalScale();
    scale.setText(sc.toString());
    // String type = (spatial instanceof Node) ? "Node" : "Geometry";

    title.setText(spatial.toString());
    Spatial rootChild = jme.getRootChild(spatial);

    /*
     * StringBuilder sb = new StringBuilder(); if (rootChild != null) {
     * sb.append(rootChild); sb.append(" > "); sb.append(spatial.getParent());
     * sb.append(" > "); sb.append(spatial); } else { sb.append(spatial); }
     */

    breadCrumbs.setText(jme.getKeyPath(spatial));
    addChildren(spatial);
  }

  public void addChildren(Spatial spatial) {
    childrenContainer.clearChildren();
    children.clear();
    boolean isNode = (spatial instanceof Node);
    if (isNode) {
      Node node = (Node) spatial;
      List<Spatial> c = node.getChildren();
      for (Spatial child : c) {
        Button b = new Button(child.toString());
        b.addClickCommands(new Command<Button>() {
          @Override
          public void execute(Button source) {
            if (child != null) {
              jme.setSelected(child);
            }
          }
        });
        // children.put(child.toString(), b);
        childrenContainer.addChild(b);
      }
      /*
       * for (String key : children.keySet()) {
       * childrenContainer.addChild(children.get(key)); }
       */
    }
  }

  public void setBreadCrumb(Spatial spatial) {
    Spatial rootChild = jme.getRootChild(spatial);
    StringBuilder sb = new StringBuilder();
    sb.append(rootChild.getName());
    sb.append(" > ");
    sb.append(spatial.getParent().getName());
    sb.append(" > ");
    sb.append(spatial.getName());
    breadCrumbs.setText(sb.toString());
  }

}
