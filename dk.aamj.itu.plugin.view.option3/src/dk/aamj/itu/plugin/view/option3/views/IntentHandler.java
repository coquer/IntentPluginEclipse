package dk.aamj.itu.plugin.view.option3.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IntentHandler {
	/* Default constructor, just in case */
	public IntentHandler() {

	}

	protected CompilationUnit parse(ICompilationUnit lwUnit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	/**
	 * Copy intent code to the Clipboard
	 * 
	 * @param intentActionName
	 * @return
	 * @throws Exception
	 */
	public void CopyIntent(String intentActionName) throws Exception {

		// Get the source
		String source = getSource(intentActionName);

		// Add to clipboard
		Display display = Display.getCurrent();
		Clipboard clipboard = new Clipboard(display);
		clipboard.setContents(new Object[] { source },
				new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();

	}

	private Block createASTFromIntentSource(String source) {

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_STATEMENTS);
		parser.setResolveBindings(false);
		return (Block) parser.createAST(null);

	}

	private String getSource(String intentActionName) {

		String formattedName = intentActionName.replace(".", "_");
		String source = "";

		try {

			URL url = new URL(
					"platform:/plugin/dk.aamj.itu.plugin.view.option3/templates/"
							+ formattedName + ".txt");
			InputStream inputStream = (InputStream) url.openConnection()
					.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					inputStream));
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				source += inputLine;
			}

			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return source;

	}

	public int InsertIntent(String intentActionName) throws Exception {

		IWorkbenchWindow wb = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPage page = wb.getActivePage();
		IEditorPart editor = page.getActiveEditor();
		IEditorInput input = editor.getEditorInput();

		ICompilationUnit compilationUnit = (ICompilationUnit) JavaUI
				.getEditorInputJavaElement(editor.getEditorInput());

		ITextEditor texteditor = (ITextEditor) editor;
		IDocument document = texteditor.getDocumentProvider()
				.getDocument(input);
		ISelection selection = texteditor.getSelectionProvider().getSelection();
		int cursorOffset = ((ITextSelection) selection).getOffset();

		CompilationUnit astRoot = parse(compilationUnit);
		astRoot.recordModifications();

		MethodDeclaration method = findMethod(cursorOffset, astRoot);
		if (method == null) {
			return 0;
		}

		int index = findIndexInMethod(method, cursorOffset);
		Block methodBody = method.getBody();

		String source = readXml(intentActionName);
		Block node = createASTFromIntentSource(source);

		for (int i = 0; i < node.statements().size(); i++) {

			ASTNode singleStmt = (ASTNode) node.statements().get(i);
			methodBody.statements().add(index + i,
					ASTNode.copySubtree(methodBody.getAST(), singleStmt));

		}

		// TODO This should also create the import * statement

		TextEdit edits = astRoot.rewrite(document, compilationUnit
				.getJavaProject().getOptions(true));
		compilationUnit.applyTextEdit(edits, null);

		compilationUnit.getBuffer().setContents(document.get());

		// Success
		return 1;

	}

	public static String readXml(String expectedStr) {
		try {

			File fXmlFile = new File("intent/intents.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("intent");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String intentNames = eElement.getAttribute("name");

					if (intentNames.equals(expectedStr)) {
						String dataField = eElement
								.getElementsByTagName("data").item(0)
								.getTextContent();
						String intentName = "Intent i = new Intent(\""
								+ eElement.getAttribute("name") + "\")";

						intentName += "\n i.putExtra(\"TODO\", extraValue)";

						if (!dataField.isEmpty()) {
							intentName += "\n i.setData(" + dataField + ")";
						}
						return intentNames;
					}

				}
			}
			return null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Find the method which the cursor is currently in
	 * 
	 * @param offset
	 * @param astRoot
	 * @return MethodDeclaration
	 */
	private MethodDeclaration findMethod(int offset, CompilationUnit astRoot) {

		MethodDeclaration methodDecl = null;

		List<BodyDeclaration> decls = ((TypeDeclaration) astRoot.types().get(0))
				.bodyDeclarations();

		for (Iterator<BodyDeclaration> iterator = decls.iterator(); iterator
				.hasNext();) {

			BodyDeclaration decl = (BodyDeclaration) iterator.next();
			if (decl instanceof MethodDeclaration) {

				methodDecl = (MethodDeclaration) decl;
				int startRange = methodDecl.getBody().getStartPosition();
				int endRange = methodDecl.getBody().getStartPosition()
						+ methodDecl.getBody().getLength();

				if (offset >= startRange && offset <= endRange)
					return methodDecl;

			}

		}

		return methodDecl;

	}

	/**
	 * Return the current statement inside a method, which the cursor currently
	 * has focus
	 * 
	 * @param method
	 * @param offset
	 * @return int
	 */
	private int findIndexInMethod(MethodDeclaration method, int offset) {

		List<Statement> statements = method.getBody().statements();

		if (statements.size() == 0)
			return 0;

		for (int i = 0; i < statements.size(); i++) {

			Statement statement = (Statement) statements.get(i);
			int startRange = statement.getStartPosition();
			int endRange = statement.getStartPosition() + statement.getLength();

			// Is cursor at the start of the method?
			if (offset <= startRange && i == 0)
				return 0;

			// Is cursor inside current statement
			if (offset >= startRange && offset <= endRange)
				return i + 1;

		}

		// Insert at end
		return statements.size();
	}

}