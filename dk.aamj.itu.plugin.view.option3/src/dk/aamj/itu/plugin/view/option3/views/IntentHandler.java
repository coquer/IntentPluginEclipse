package dk.aamj.itu.plugin.view.option3.views;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class IntentHandler {
	/* Default constructor, just in case */
	public IntentHandler(){
		
	}
	
	public String insertIntent(String instanceName, String parameter) throws Exception{
		IWorkbenchWindow wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage tp = wp.getActivePage();
		IEditorPart editor = tp.getActiveEditor();
		IEditorInput crInput = editor.getEditorInput();
		
		
		
		
		
		return instanceName;
	}//closes insertIntent method
	
















}//closes class 