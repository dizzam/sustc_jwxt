package per.dizzam.sustc.jwxt.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.http.auth.AuthenticationException;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import per.dizzam.sustc.jwxt.CourseData;
import per.dizzam.sustc.jwxt.StatusException;
import per.dizzam.sustc.jwxt.CourseData.CourseRepo;

public class Main extends Shell {

	public static CourseData courseData;
	private SashForm sashForm;
	private Tree tree;
	private static String[] WEEK = new String[] { "", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
	private Text text;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			try {
				PropertyConfigurator.configure(Main.class.getResourceAsStream("/log4j.properties"));	
			} catch (NullPointerException e) {
				PropertyConfigurator.configure("log4j.properties");
			}
			courseData = new CourseData();
			Display display = Display.getDefault();
			Main shell = new Main(display);
			login(shell);
			shell.createContents();
			shell.open();
			shell.layout();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			courseData.saveToFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean login(Shell shell) {
		Login dialog = new Login(shell, SWT.PRIMARY_MODAL);
		do {
			try {
				Object o = dialog.open();
				if (o != null) {
					String[] auth = (String[]) o;
					courseData.login(auth[0], auth[1]);
					break;
				}
				return false;
			} catch (AuthenticationException e) {
				MessageBox messageBox = new MessageBox(shell, SWT.OK);
				messageBox.setMessage(e.getMessage());
				messageBox.open();
				if (e.getCause() instanceof IOException) {
					return false;
				}
			}
		} while (true);
		return true;
	}

	/**
	 * Create the shell.
	 * 
	 * @param display
	 */
	public Main(Display display) {
		super(display, SWT.SHELL_TRIM);
		setSize(900, 700);
		setLayout(new FormLayout());

		SashForm sashForm_p = new SashForm(this, SWT.NONE);
		FormData fd_sashForm_p = new FormData();
		fd_sashForm_p.bottom = new FormAttachment(100, -20);
		fd_sashForm_p.top = new FormAttachment(0, 15);
		fd_sashForm_p.right = new FormAttachment(100, -20);
		fd_sashForm_p.left = new FormAttachment(0, 20);
		sashForm_p.setLayoutData(fd_sashForm_p);

		Group group = new Group(sashForm_p, SWT.NONE);
		group.setLayout(new FormLayout());
		group.setText("课程");

		tree = new Tree(group, SWT.BORDER | SWT.CHECK);
		tree.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.item instanceof TreeItem) {
					TreeItem item = (TreeItem) e.item;
					boolean checked = item.getChecked();
					item.setChecked(checked);
					item.setGrayed(false);
					Stack<TreeItem> items = new Stack<>();
					items.push(item);
					while (!items.isEmpty()) {
						TreeItem item1 = items.pop();
						item1.setChecked(checked);
						item1.setGrayed(false);
						items.addAll(Arrays.asList(item1.getItems()));
					}
					TreeItem parent;
					TreeItem head = item;
					while ((parent = head.getParentItem()) != null) {
						if (checked) {
							parent.setChecked(true);
							parent.setGrayed(false);
							for (TreeItem item1 : parent.getItems()) {
								if (!item1.getChecked() || item1.getGrayed()) {
									parent.setGrayed(true);
									break;
								}
							}
						} else {
							parent.setGrayed(true);
							parent.setChecked(false);
							for (TreeItem item1 : parent.getItems()) {
								if (item1.getChecked()) {
									parent.setChecked(true);
									break;
								}
							}
						}
						head = parent;
					}
				}
			}
		});
		FormData fd_tree = new FormData();
		fd_tree.bottom = new FormAttachment(100, -7);
		fd_tree.right = new FormAttachment(100, -7);
		fd_tree.top = new FormAttachment(0, 30);
		fd_tree.left = new FormAttachment(0, 7);
		tree.setLayoutData(fd_tree);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		TreeColumn trclmnA = new TreeColumn(tree, SWT.NONE);
		trclmnA.setWidth(275);
		trclmnA.setText("课程名称");

		TreeColumn treeColumn = new TreeColumn(tree, SWT.NONE);
		treeColumn.setWidth(36);
		treeColumn.setText("学分");

		TreeColumn trlclmn_ls = new TreeColumn(tree, SWT.NONE);
		trlclmn_ls.setWidth(60);
		trlclmn_ls.setText("老师");

		TreeColumn trlclmn_pgtj = new TreeColumn(tree, SWT.NONE);
		trlclmn_pgtj.setWidth(275);
		trlclmn_pgtj.setText("先修课程");

		Button button = new Button(group, SWT.NONE);
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				do {
					try {
						courseData.updateCourseData();
						courseData.updateSelected();
						break;
					} catch (AuthenticationException e1) {
						if (!login(Main.this)) {
							break;
						}
					} catch (StatusException e1) {
						MessageBox messageBox = new MessageBox(Main.this, SWT.OK);
						messageBox.setMessage(e1.getMessage());
						messageBox.open();
						break;
					}
				} while (true);
				updateData(null);
			}
		});
		FormData fd_button = new FormData();
		fd_button.right = new FormAttachment(tree, 0, SWT.RIGHT);
		button.setLayoutData(fd_button);
		button.setText("更新数据");

		text = new Text(group, SWT.BORDER);
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				updateData(text.getText().equals("") ? null : text.getText());
			}
		});
		FormData fd_text = new FormData();
		fd_text.top = new FormAttachment(button, 2, SWT.TOP);
		fd_text.left = new FormAttachment(tree, 0, SWT.LEFT);
		text.setLayoutData(fd_text);

		Button button_1 = new Button(group, SWT.NONE);
		button_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				updateData(text.getText().equals("") ? null : text.getText());
			}
		});
		FormData fd_button_1 = new FormData();
		fd_button_1.left = new FormAttachment(0, 86);
		fd_button_1.top = new FormAttachment(button, 0, SWT.TOP);
		button_1.setLayoutData(fd_button_1);
		button_1.setText("搜索");

		ScrolledComposite scrolledComposite = new ScrolledComposite(sashForm_p, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new FormData());
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setMinWidth(0);

		sashForm = new SashForm(scrolledComposite, SWT.HORIZONTAL | SWT.BORDER);
		sashForm.setEnabled(false);

		for (String string : WEEK) {
			SashForm sashForm_1 = new SashForm(sashForm, SWT.VERTICAL);
			sashForm_1.setEnabled(false);

			Label lbl = new Label(sashForm_1, SWT.CENTER);
			lbl.setText(string);

			Composite composite2 = new Composite(sashForm_1, SWT.NONE);
			composite2.setLayout(new FormLayout());

			for (int i = 0; i < 10; i++) {
				Label line = new Label(composite2, SWT.SEPARATOR | SWT.HORIZONTAL);

				FormData fd_line = new FormData();
				fd_line.left = new FormAttachment(0, 0);
				fd_line.right = new FormAttachment(100, 0);
				fd_line.top = new FormAttachment(i * 10, 0);
				line.setLayoutData(fd_line);
				if (string.equals("")) {
					Label num = new Label(composite2, SWT.CENTER);
					FormData fd_num = new FormData();
					fd_num.left = new FormAttachment(0, 0);
					fd_num.right = new FormAttachment(100, 0);
					fd_num.top = new FormAttachment(i * 10 + 4, 0);
					num.setLayoutData(fd_num);
					num.setText(String.valueOf(i + 1));
				}
			}
			sashForm_1.setWeights(new int[] { 1, 40 });
		}

		scrolledComposite.setContent(sashForm);

		sashForm.setWeights(new int[] { 2, 9, 9, 9, 9, 9, 9, 9 });
		sashForm_p.setWeights(new int[] { 3, 5 });

	}

	/**
	 * Create contents of the shell.
	 */
	protected void createContents() {
		setText("App");
		setMaximized(true);

		updateData(null);
	}

	private void updateData(String string) {
		tree.removeAll();
		JsonObject result = string == null ? courseData.getCourse() : courseData.search(string);
		for (Entry<String, JsonElement> entry : result.entrySet()) {
			for (JsonElement element : entry.getValue().getAsJsonArray()) {
				TreeItem root = null;
				for (TreeItem item : tree.getItems()) {
					if (item.getText().equals(CourseRepo.valueOf(entry.getKey()).getName())) {
						root = item;
					}
				}
				if (root == null) {
					TreeItem newItem = new TreeItem(tree, SWT.CHECK);
					newItem.setText(CourseRepo.valueOf(entry.getKey()).getName());
					root = newItem;
				}
				TreeItem parent = null;
				for (TreeItem item : root.getItems()) {
					if (item.getText().equals(element.getAsJsonObject().get("kcmc").getAsString())) {
						parent = item;
					}
				}
				if (parent == null) {
					TreeItem newItem = new TreeItem(root, SWT.NONE);
					newItem.setText(0, element.getAsJsonObject().get("kcmc").getAsString());
					newItem.setText(1, String.valueOf(element.getAsJsonObject().get("xf").getAsInt()));
					JsonElement e = element.getAsJsonObject().get("pgtj");
					newItem.setText(3, e.isJsonNull() ? "无" : e.getAsString());
					parent = newItem;
				}
				TreeItem item = new TreeItem(parent, SWT.NONE);
				JsonElement e1 = element.getAsJsonObject().get("fzmc");
				item.setText(0, element.getAsJsonObject().get("kcmc").getAsString()
						+ (e1.isJsonNull() ? "" : "[" + e1.getAsString() + "]"));
				item.setText(1, String.valueOf(element.getAsJsonObject().get("xf").getAsInt()));
				JsonElement e2 = element.getAsJsonObject().get("skls");
				item.setText(2, e2.isJsonNull() ? "无" : e2.getAsString());
				JsonElement e3 = element.getAsJsonObject().get("pgtj");
				item.setText(3, e3.isJsonNull() ? "无" : e3.getAsString());
			}
		}
		for (TreeItem item : tree.getItems()) {
			item.setExpanded(true);
		}
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}