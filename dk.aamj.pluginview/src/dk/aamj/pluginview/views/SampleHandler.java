package dk.aamj.pluginview.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;


//extends AbstractHandler
public class SampleHandler   {

//  public Object execute(ExecutionEvent event) throws ExecutionException {
//    
//    try {
//		InsertIntent("i", "com.smartmadsoft.openwatch.command");
//	} catch (Exception e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
//    
//    return null;
//  }
  
  public SampleHandler(){
	  
  }
  
	protected CompilationUnit parse(ICompilationUnit lwUnit) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(lwUnit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}
  
  public void InsertIntent(String instanceName, String parameter) throws Exception{
  
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
		if(method == null){
			return;
		}
		
		int index = findIndexInMethod(method, cursorOffset);
		List statementsList = method.getBody().statements();
		
		VariableDeclarationFragment vdf = astRoot.getAST().newVariableDeclarationFragment();  
		vdf.setName(astRoot.getAST().newSimpleName(instanceName));  
		ClassInstanceCreation cc = astRoot.getAST().newClassInstanceCreation();  
		cc.setType(astRoot.getAST().newSimpleType(astRoot.getAST().newSimpleName("Intent")));
		StringLiteral l = astRoot.getAST().newStringLiteral();
		l.setLiteralValue(parameter);
		cc.arguments().add(l);
		vdf.setInitializer(cc);
		VariableDeclarationStatement vds = astRoot.getAST().newVariableDeclarationStatement(vdf);  
		vds.setType(astRoot.getAST().newSimpleType(astRoot.getAST().newSimpleName("Intent")));
		
			
		   
		statementsList.add(index+1, vds);
		
		
	  
		
		ASTRewrite rewriter = ASTRewrite.create(astRoot.getAST());
		
				
		Block block = method.getBody();
 		
		TextEdit edits = astRoot.rewrite(document, compilationUnit.getJavaProject().getOptions(true));
		compilationUnit.applyTextEdit(edits, null);
 
		compilationUnit.getBuffer().setContents(document.get());
		
  }
  
  private void getCursorPosition(){
		IWorkbenchWindow wb = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = wb.getActivePage();
		IEditorPart editor = page.getActiveEditor();
		IEditorInput input = editor.getEditorInput();

		ITextEditor texteditor = (ITextEditor) editor;
		IDocument document = texteditor.getDocumentProvider().getDocument(input);
		ISelection selection = texteditor.getSelectionProvider().getSelection();
		int cursorOffset = ((ITextSelection) selection).getOffset();
  }
  
  private MethodDeclaration findMethod(int offset, CompilationUnit astRoot){
		MethodDeclaration methodDecl = null;
		
		List decls =((TypeDeclaration)astRoot.types().get(0)).bodyDeclarations();
		for (Iterator iterator = decls.iterator(); iterator.hasNext();){
			BodyDeclaration decl = (BodyDeclaration) iterator.next();
			if(decl instanceof MethodDeclaration){
				methodDecl = (MethodDeclaration) decl;
				int startRange = methodDecl.getBody().getStartPosition();
				int endRange = methodDecl.getBody().getStartPosition() + methodDecl.getBody().getLength();
				if(offset > startRange && offset < endRange){
					return methodDecl;
				}
			}
		}
		return methodDecl;
	}
  
  private int findIndexInMethod(MethodDeclaration method, int offset){
		int index = -1;
		
		Block body = method.getBody();
		List statements = body.statements();
		for(int i = 0; i < statements.size(); i++){
			Statement statement = (Statement) statements.get(i);
			int startRange = statement.getStartPosition();
			int endRange = statement.getStartPosition() + statement.getLength();
			if(offset > startRange){
				if(offset < endRange){
					return i;
				}
			}else{
				return i;
			}
		}
		
		return index;
	}  
} 