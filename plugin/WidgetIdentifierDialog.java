package plugin;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import com.alee.laf.button.WebButton;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.tabbedpane.WebTabbedPane;
import com.alee.laf.text.WebTextField;

import scout.StateController;

public class WidgetIdentifierDialog extends JDialog implements ActionListener
{
	private static final long serialVersionUID = 2280179929967793363L;

	private Frame parent;
	private WebTextField elementsToExtractTextField=new WebTextField();
	private WebTextField containerTagsTextField=new WebTextField();
	private WebTextField frameTagsTextField=new WebTextField();
	private WebTextField clickTagsTextField=new WebTextField();
	private WebTextField clickInputTypesTextField=new WebTextField();
	private WebTextField clickClassesTextField=new WebTextField();
	private WebTextField checkTagsTextField=new WebTextField();
	private WebTextField checkInputTypesTextField=new WebTextField();
	private WebTextField checkClassesTextField=new WebTextField();
	private WebTextField typeTagsTextField=new WebTextField();
	private WebTextField typeInputTypesTextField=new WebTextField();
	private WebTextField typeClassesTextField=new WebTextField();
	private WebTextField moveTagsTextField=new WebTextField();
	private WebTextField moveClassesTextField=new WebTextField();
	private WebTextField selectTagsTextField=new WebTextField();
	private WebTextField selectClassesTextField=new WebTextField();
	private WebButton applyButton;
	private WebButton saveButton;
	private WebButton closeButton;

	private static final int WIDTH = 600;
	private static final int HEIGHT = 350;

	public WidgetIdentifierDialog(Frame parent)
	{
		super(parent, "Edit Widget Identifiers", false);
		this.parent=parent;

		ActionListener escListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				disposeDialog();
			}
		};
		this.getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		// Center dialog on parent
		setSize(WIDTH, HEIGHT);
		setResizable(false);
	}
	
	public boolean showDialog()
	{
		if(!createGui())
		{
			return false;
		}
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		return true;
	}

	public void disposeDialog()
	{
		setVisible(false);
		dispose();
	}

	private boolean createGui()
	{
		addWindowListener(new WindowEventHandler());
		getContentPane().setLayout(new BorderLayout());

		WebTabbedPane tabbedPane = new WebTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		WebPanel filterPanel = new WebPanel();
		filterPanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Filter", filterPanel);

		WebPanel clickPanel = new WebPanel();
		clickPanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Click", clickPanel);

		WebPanel checkPanel = new WebPanel();
		checkPanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Check", checkPanel);

		WebPanel typePanel = new WebPanel();
		typePanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Type", typePanel);

		WebPanel movePanel = new WebPanel();
		movePanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Move", movePanel);

		WebPanel selectPanel = new WebPanel();
		selectPanel.setLayout(new BorderLayout());
		tabbedPane.addTab("Select", selectPanel);

		filterPanel.add(createTextField(elementsToExtractTextField, "Tags to extract: ", StateController.getProductProperty("elements_to_extract", WidgetIdentifier.ELEMENTS_TO_EXTRACT)), BorderLayout.NORTH);
		filterPanel.add(createTextField(containerTagsTextField, "Container tags: ", StateController.getProductProperty("container_tags", WidgetIdentifier.CONTAINER_TAGS)), BorderLayout.CENTER);
		filterPanel.add(createTextField(frameTagsTextField, "Frame tags: ", StateController.getProductProperty("frame_tags", WidgetIdentifier.FRAME_TAGS)), BorderLayout.SOUTH);

		clickPanel.add(createTextField(clickTagsTextField, "Click tags: ", StateController.getProductProperty("click_tags", WidgetIdentifier.CLICK_TAGS)), BorderLayout.NORTH);
		clickPanel.add(createTextField(clickInputTypesTextField, "Click input types: ", StateController.getProductProperty("click_input_types", WidgetIdentifier.CLICK_INPUT_TYPES)), BorderLayout.CENTER);
		clickPanel.add(createTextField(clickClassesTextField, "Click classes: ", StateController.getProductProperty("click_classes", null)), BorderLayout.SOUTH);

		checkPanel.add(createTextField(checkTagsTextField, "Check tags: ", StateController.getProductProperty("check_tags", WidgetIdentifier.CHECK_TAGS)), BorderLayout.NORTH);
		checkPanel.add(createTextField(checkInputTypesTextField, "Check input types: ", StateController.getProductProperty("check_input_types", WidgetIdentifier.CHECK_INPUT_TYPES)), BorderLayout.CENTER);
		checkPanel.add(createTextField(checkClassesTextField, "Check classes: ", StateController.getProductProperty("check_classes", null)), BorderLayout.SOUTH);

		typePanel.add(createTextField(typeTagsTextField, "Type tags: ", StateController.getProductProperty("type_tags", WidgetIdentifier.TYPE_TAGS)), BorderLayout.NORTH);
		typePanel.add(createTextField(typeInputTypesTextField, "Type input types: ", StateController.getProductProperty("type_input_types", WidgetIdentifier.TYPE_INPUT_TYPES)), BorderLayout.CENTER);
		typePanel.add(createTextField(typeClassesTextField, "Type classes: ", StateController.getProductProperty("type_classes", null)), BorderLayout.SOUTH);

		movePanel.add(createTextField(moveTagsTextField, "Move tags: ", StateController.getProductProperty("move_tags", WidgetIdentifier.MOVE_TAGS)), BorderLayout.NORTH);
		movePanel.add(createTextField(moveClassesTextField, "Move classes: ", StateController.getProductProperty("move_classes", null)), BorderLayout.CENTER);

		selectPanel.add(createTextField(selectTagsTextField, "Select tags: ", StateController.getProductProperty("select_tags", WidgetIdentifier.SELECT_TAGS)), BorderLayout.NORTH);
		selectPanel.add(createTextField(selectClassesTextField, "Select classes: ", StateController.getProductProperty("select_classes", null)), BorderLayout.CENTER);

		// Button panel
		WebPanel buttonPanel = new WebPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		applyButton = createButton(this, "Apply");
		saveButton = createButton(this, "Save");
		closeButton = createButton(this, "Close");

    buttonPanel.add(applyButton);
    buttonPanel.add(saveButton);
    buttonPanel.add(closeButton);

    return true;
	}
	
	private WebPanel createTextField(WebTextField textField, String label, String text)
	{
		textField.setMinimumWidth(500);
		textField.addActionListener(this);
		textField.setText(text);

		WebPanel panel = new WebPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(new JLabel(label));
		panel.add(textField);
		
		return panel;
	}

	class WindowEventHandler extends WindowAdapter
	{
		public void windowClosing(WindowEvent e)
		{
			disposeDialog();
		}

		public void windowOpened(WindowEvent e)
		{
			saveButton.requestFocus();
		}
	}

	private WebButton createButton(Container parent, String label)
	{
		WebButton button = new WebButton(label);
		button.addActionListener((ActionListener) parent);
		return button;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == applyButton)
		{
			saveSettings();
		}
		if (e.getSource() == saveButton)
		{
			saveSettings();
			disposeDialog();
		}
		if (e.getSource() == closeButton)
		{
			disposeDialog();
		}
	}

	private void saveSettings()
	{
		WidgetIdentifier.setElementsToExtract(elementsToExtractTextField.getText());
		WidgetIdentifier.setContainerTags(containerTagsTextField.getText());
		WidgetIdentifier.setFrameTags(frameTagsTextField.getText());

		WidgetIdentifier.setClickTags(clickTagsTextField.getText());
		WidgetIdentifier.setClickInputTypes(clickInputTypesTextField.getText());
		WidgetIdentifier.setClickClasses(clickClassesTextField.getText());

		WidgetIdentifier.setCheckTags(checkTagsTextField.getText());
		WidgetIdentifier.setCheckInputTypes(checkInputTypesTextField.getText());
		WidgetIdentifier.setCheckClasses(checkClassesTextField.getText());

		WidgetIdentifier.setTypeTags(typeTagsTextField.getText());
		WidgetIdentifier.setTypeInputTypes(typeInputTypesTextField.getText());
		WidgetIdentifier.setTypeClasses(typeClassesTextField.getText());

		WidgetIdentifier.setMoveTags(moveTagsTextField.getText());
		WidgetIdentifier.setMoveClasses(moveClassesTextField.getText());

		WidgetIdentifier.setSelectTags(selectTagsTextField.getText());
		WidgetIdentifier.setSelectClasses(selectClassesTextField.getText());
	}
}
