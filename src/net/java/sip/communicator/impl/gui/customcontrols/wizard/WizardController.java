/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * This class is responsible for reacting to events generated by pushing any
 * of the three buttons, 'Next', 'Previous', and 'Cancel.' Based on what
 * button is pressed, the controller will update the model to show a new panel
 * and reset the state of the buttons as necessary.
 * 
 * @author Yana Stamcheva
 */
public class WizardController implements ActionListener
{
    private Wizard wizard;

    /**
     * This constructor accepts a reference to the Wizard component that created
     * it, which it uses to update the button components and access the
     * WizardModel.
     * @param w A callback to the Wizard component that created this controller.
     */
    public WizardController(Wizard w) {
        wizard = w;
    }

    /**
     * Calling method for the action listener interface. This class listens for
     * actions performed by the buttons in the Wizard class, and calls methods
     * below to determine the correct course of action.
     * @param evt The ActionEvent that occurred.
     */
    public void actionPerformed(java.awt.event.ActionEvent evt) {

        if (evt.getActionCommand().equals(
                Wizard.CANCEL_BUTTON_ACTION_COMMAND))
            cancelButtonPressed();
        else if (evt.getActionCommand().equals(
                Wizard.BACK_BUTTON_ACTION_COMMAND))
            backButtonPressed();
        else if (evt.getActionCommand().equals(
                Wizard.NEXT_BUTTON_ACTION_COMMAND))
            nextButtonPressed();

    }

    /**
     * Closes the wizard by specifying the appropriate return code, when user
     * has pressed the "Cancel" button.
     */
    private void cancelButtonPressed() {

        wizard.close(Wizard.CANCEL_RETURN_CODE);
    }

    /**
     * If it is a finishable panel, closes the dialog. Otherwise, gets the
     * ID that the current panel identifies as the next panel, and displays
     * the panel that it's identifying.
     */
    private void nextButtonPressed()
    {
        wizard.startCommittingPage();

        new Thread(new PageCommitThread()).start();
    }

    /**
     * Gets the descriptor that the current panel identifies as the previous
     * panel, and displays the panel that it's identifying.
     */
    private void backButtonPressed() {

        WizardModel model = wizard.getModel();
        WizardPage page = model.getCurrentWizardPage();

        page.pageBack();

        Object backPageIdentifier = page.getBackPageIdentifier();

        wizard.setCurrentPage(backPageIdentifier);
    }

    /**
     *  Resets the buttons to support the original panel rules, including
     *  whether the next or back buttons are enabled or disabled, or if
     *  the panel is finish-able. If the panel in question has another panel
     *  behind it, enables the back button. Otherwise, disables it. If the
     *  panel in question has one or more panels in front of it, enables the
     *  next button. Otherwise, disables it.
     */
    void resetButtonsToPanelRules()
    {
        WizardModel model = wizard.getModel();
        WizardPage page = model.getCurrentWizardPage();

        model.setCancelButtonText(wizard.getCancelButtonDefaultText());

        Object backPageIdentifier = page.getBackPageIdentifier();
        model.setBackButtonEnabled(
            (backPageIdentifier != null)
                && !WizardPage.DEFAULT_PAGE_IDENTIFIER.equals(backPageIdentifier));

        model.setBackButtonText(wizard.getBackButtonDefaultText());

        model.setNextFinishButtonEnabled(page.getNextPageIdentifier() != null);

        if (page.getNextPageIdentifier().equals(
                WizardPage.FINISH_PAGE_IDENTIFIER)) {
            model.setNextFinishButtonText(wizard.getFinishButtonDefaultText());
        } else {
            model.setNextFinishButtonText(wizard.getNextButtonDefaultText());
        }
    }

    /**
     * Runs committing page in new thread to avoid blocking UI thread.
     */
    private class PageCommitThread
        implements Runnable
    {
        /**
         * Commits wizard page.
         */
        public void run()
        {
            WizardModel model = wizard.getModel();
            final WizardPage page = model.getCurrentWizardPage();

            try
            {
                page.commitPage();
            }
            catch (Exception ex)
            {
                wizard.stopCommittingPage();

                //lots of things may fail on page next, like for example parameter
                //validation or account initialization. If this is what happened here
                //just show an error and leave everything on the same page so that
                //the user would have the chance to correct errors.
                new ErrorDialog(
                    null,
                    GuiActivator.getResources().getI18NString("service.gui.ERROR"),
                    ex.getMessage(),
                    ex).showDialog();
                return;
            }

            wizard.stopCommittingPage();

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    Object nextPageIdentifier = page.getNextPageIdentifier();

                    if (nextPageIdentifier
                            .equals(WizardPage.FINISH_PAGE_IDENTIFIER))
                    {
                        wizard.close(Wizard.FINISH_RETURN_CODE);
                    }
                    else
                    {
                        wizard.setCurrentPage(nextPageIdentifier);
                    }
                }
            });
        }
    }
}
