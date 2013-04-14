package dk.aamj.itu.plugin.view.option3.views;

import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;



public class IntentHandler {
	/* Default constructor, just in case */
	public IntentHandler(){

	}

	protected CompilationUnit parse(ICompilationUnit lwUnit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}
	
	public void CopyIntent(String instanceName, String parameter) throws Exception {
		
		// Make the intent code and copy it to the clipboard
		Display display = Display.getCurrent();
        Clipboard clipboard = new Clipboard(display);
        clipboard.setContents(new Object[] { "this is my text" }, new Transfer[] { TextTransfer.getInstance() });
        clipboard.dispose();
		
	}
	
	private Block createASTFromIntentSource(String source) {
			
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_STATEMENTS);
		parser.setResolveBindings(false);
		return (Block) parser.createAST(null);
//		CompilationUnit result = (CompilationUnit) parser.createAST(null);
//		return result;
		
	}

	public int InsertIntent(String instanceName, String parameter) throws Exception {
		
		IWorkbenchWindow wb = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = wb.getActivePage();
		IEditorPart editor = page.getActiveEditor();
		IEditorInput input = editor.getEditorInput();

		ICompilationUnit compilationUnit = (ICompilationUnit) JavaUI.getEditorInputJavaElement(editor.getEditorInput());

		ITextEditor texteditor = (ITextEditor) editor;
		IDocument document = texteditor.getDocumentProvider().getDocument(input);
		ISelection selection = texteditor.getSelectionProvider().getSelection();
		int cursorOffset = ((ITextSelection) selection).getOffset();

		CompilationUnit astRoot = parse(compilationUnit);
		astRoot.recordModifications();

		MethodDeclaration method = findMethod(cursorOffset, astRoot);
		if(method == null) {
			return 0;
		}

		int index = findIndexInMethod(method, cursorOffset);
		Block methodBody = method.getBody();
		
		String source = "Intent i = new Intent(\"com.action.ash.needs.coffee\");i.setData(\"text/plain\");";
		Block node = createASTFromIntentSource(source);
		
		for (int i = 0; i < node.statements().size(); i++) {

			ASTNode singleStmt = (ASTNode) node.statements().get(i);
			methodBody.statements().add(index + i, ASTNode.copySubtree(methodBody.getAST(), singleStmt));

		}
		
		ASTRewrite rewriter = ASTRewrite.create(astRoot.getAST());

		TextEdit edits = astRoot.rewrite(document, compilationUnit.getJavaProject().getOptions(true));
		compilationUnit.applyTextEdit(edits, null);

		compilationUnit.getBuffer().setContents(document.get());

		// Success
		return 1;

	}

	/**
	 * Find the method which the cursor is currently in
	 * @param offset
	 * @param astRoot
	 * @return MethodDeclaration
	 */
	private MethodDeclaration findMethod(int offset, CompilationUnit astRoot) {
		
		MethodDeclaration methodDecl = null;

		List<BodyDeclaration> decls = ((TypeDeclaration)astRoot.types().get(0)).bodyDeclarations();
		
		for (Iterator<BodyDeclaration> iterator = decls.iterator(); iterator.hasNext();){
			
			BodyDeclaration decl = (BodyDeclaration) iterator.next();
			if(decl instanceof MethodDeclaration) {
				
				methodDecl = (MethodDeclaration) decl;
				int startRange = methodDecl.getBody().getStartPosition();
				int endRange = methodDecl.getBody().getStartPosition() + methodDecl.getBody().getLength();
				
				if(offset >= startRange && offset <= endRange)
					return methodDecl;
				
			}
			
		}
		
		return methodDecl;
		
	}

	/**
	 * Return the current statement inside a method, which the cursor currently has focus
	 * @param method
	 * @param offset
	 * @return int
	 */
	private int findIndexInMethod(MethodDeclaration method, int offset){

		List<Statement> statements = method.getBody().statements();
		
		if(statements.size() == 0)
			return 0;
		
		for(int i = 0; i < statements.size(); i++){
			
			Statement statement = (Statement) statements.get(i);
			int startRange = statement.getStartPosition();
			int endRange = statement.getStartPosition() + statement.getLength();
			
			//Is cursor at the start of the method?
			if(offset <= startRange && i == 0)
				return 0;
			
			// Is cursor inside current statement
			if(offset >= startRange && offset <= endRange)
				return i+1;
			
		}

		// Insert at end
		return statements.size();
	}
	
} 