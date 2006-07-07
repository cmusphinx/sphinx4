
/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package demo.jsapi.cardmatch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 * The GUI class for the CardMatch demo.
 */
public class CardMatchFrame extends JFrame {

    private Dimension dimension = new Dimension(800, 700);
    private Color backgroundColor = new Color(220, 220, 220);

    private JPanel cardPanel;
    private JButton newGameButton;
    private JToggleButton speakButton;
    private JTextField resultsTextField;

    private Recorder recorder;
    
    private Game game;
    private List cards;
    private Map buttonMap;
    private CardMatchVoice voice;

    private String[] goodGuessText =
        {
        "Good guess",
        "Nice job",
         "Super, one more to go.",
         "You got it!",
         "Good for you!",
         "Way to go!"};

    private Prompt goodGuessPrompt = new SequencePrompt(goodGuessText);

    private String[] badGuessText =
        {"I'm sorry!", 
         "Wrong again!",
         "Make another guess",
         "One more time",
         "Sorry",
         "think harder",
         "you can do better",
        };

    private Prompt badGuessPrompt = new SequencePrompt(badGuessText);

    private String[] victoryText = {
	"Congratulations, You have won the game."
    };

    private Prompt victoryPrompt = new SequencePrompt(victoryText);
         
    private String[] newGameText =
        { "Let's play a game. Go ahead and pick a card" };

    private Prompt newGamePrompt = new SequencePrompt(newGameText);

    private String[] noInputText =
        {
            "I'm sorry, I didn't here you.",
         "Excuse me?",
         "Please say that again.",
         "Pardon me?",
         "I can't hear you too well." ,
         "Say again please",
         "What?" 
        };

    private Prompt noInputPrompt = new SequencePrompt(noInputText);



    /**
     * Constructs a CardMatchFrame with the given title.
     *
     * @param title the title of the frame
     * @param recorder the recorder 
     * @param game the game
     */
    public CardMatchFrame(String title, Recorder recorder, Game game,
                          boolean useVoice) {
	super(title);
	this.recorder = recorder;
        this.game = game;
        this.cards = game.getCards();

        if (useVoice) {
            try {
                voice = new CardMatchVoice();
                System.out.println("   Loaded synthesizer voice");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	setSize(dimension);
	getContentPane().add(createMainPanel(), BorderLayout.CENTER);

	// add a listener for closing this JFrame and quitting the program
        addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });
        startNewGame(game);
    }

    /**
     * Updates the card panel.
     */
    private void newCardPanel() {
        cardPanel.removeAll();
        buttonMap = new HashMap();
        // add the check-boxes
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            cardPanel.add(createCardButton(card));
        }
        validate();
        repaint();
    }


    /**
     * Creates the button associated with the given card
     *
     * @param card the card that the button will be created for
     *
     * @return the button
     */
    private JToggleButton createCardButton(final Card card) {
        JToggleButton button = getCardButton(card);

        if (button == null) {
            button = new JToggleButton("Card " + card.getID(), 
                     new ImageIcon(card.getDefaultImageFile()));
            button.setSelectedIcon(new ImageIcon(card.getImageFile()));
            button.setBorderPainted(true);
            button.setContentAreaFilled(false);
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.setVerticalAlignment(SwingConstants.CENTER);
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JToggleButton source = (JToggleButton) e.getSource();
                        handleCards(card, source.isSelected());
                    }
            });
            buttonMap.put(card, button);
        }
        return button;
    }


    /**
     * Updates the button associated with a card
     *
     * @param card the button associated with this card is updated.
     */
    private void updateCard(Card card) {
        JToggleButton button = getCardButton(card);
        button.setSelected(card.isSelected());
    }

    /**
     * Gets the button associated with the given card
     *
     * @param card the card 
     *
     * @return the button associated with the card
     */
    private JToggleButton getCardButton(Card card) {
        return (JToggleButton) buttonMap.get(card);
    }

    /**
     * Updates all of the cards
     */
    private void updateAllCards() {
        for (Iterator i = cards.iterator(); i.hasNext(); ) {
            Card card = (Card) i.next();
            updateCard(card);
        }
    }

    /**
     * Starts a new game
     *
     * @param game the game to start over
     */
    public void startNewGame(Game game) {
        resetPrompts();
        game.startOver();
        newCardPanel();
        speak(newGamePrompt);
        speakButton.setSelected(false);
    }


    /**
     * Returns a JPanel with the custom background color.
     *
     * @param manager the layout manager to use.
     * @return a JPanel
     */
    private JPanel getJPanel(LayoutManager manager) {
        JPanel panel = new JPanel();
        panel.setBackground(backgroundColor);
	panel.setLayout(manager);
        return panel;
    }


    /**
     * Creates a JTextArea for the Statistics text.
     *
     * @param text the text for the text area
     *
     * @return a JTextArea for the Statistics text
     */
    private JTextArea getTextArea(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(backgroundColor);
        textArea.setAlignmentX(JTextArea.RIGHT_ALIGNMENT);
        return textArea;
    }


    /**
     * Creates the main panel that contains the top
     * card and buttons panels, and the bottom results panel.
     *
     * @return the main panel
     */
    private JPanel createMainPanel() {
	JPanel mainPanel = getJPanel(new BorderLayout());
	mainPanel.add(createCardPanel(), BorderLayout.CENTER);
	mainPanel.add(createResultsPanel(), BorderLayout.SOUTH);
	return mainPanel;
    }


    /**
     * Creates the card panel.
     *
     * @return the card panel
     */
    private JPanel createCardPanel() {
	cardPanel = getJPanel(new GridLayout(2, 3));
        assert cards.size() == 6;
        newCardPanel();
     	return cardPanel;
    }


    /**
     * Handles the pressing of the "Speak" button.
     */
    private void speakButtonPressed() {
	if (!recorder.isRecording()) {
	    if (!recorder.startRecording()) {
		System.out.println("Error turning microphone on.");
                speakButton.setSelected(false);
	    }
	}
    }


    /**
     * Handles the pressing of the "Stop" button.
     */
    private void stopButtonPressed() {
	if (recorder.isRecording()) {
	    if (!recorder.stopRecording()) {
		System.out.println("Error turning microphone off.");
	    }
	}
    }


    /**
     * Creates the results panel.
     *
     * @return the panel
     */
    private JPanel createResultsPanel() {
	JPanel resultsPanel = getJPanel(new BorderLayout());
	JTextArea textArea = getTextArea("You said: ");
	
       	resultsTextField = new JTextField();

	resultsPanel.add(textArea, BorderLayout.WEST);
	resultsPanel.add(resultsTextField, BorderLayout.CENTER);

        JPanel buttonsPanel = getJPanel(new FlowLayout());
        
	speakButton = new JToggleButton("Speak");
        speakButton.requestFocus();
	speakButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (speakButton.isSelected()) {
                    speakButtonPressed();
		} else {
                    stopButtonPressed();
                }
	    }
        });

        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopButtonPressed();
                    startNewGame(game);
                }
            });

        buttonsPanel.add(speakButton);
        buttonsPanel.add(newGameButton);

        resultsPanel.add(buttonsPanel, BorderLayout.EAST);

	return resultsPanel;
    }


    /**
     * Speaks the given string of text.
     *
     * @param text the text to speak
     */
    private void speak(String text) {
        if (voice != null) {
            voice.speak(text);
        } else {
	    System.out.println("Speak: " + text);
	}
    }

    /**
     * Speaks the given prompt
     *
     * @param prompt the prompt to speak
     */
    private void speak(Prompt prompt) {
        if (voice != null) {
            voice.speak(prompt.getText());
        } else {
            System.out.println("Speak: " + prompt.getText());
        }
    }


    /**
     * Process the given results
     *
     * @param text the spoken text
     * @param tag the tag associated with the text
     */
    void processResults(final String text, final String tag) {
        try {
            SwingUtilities.invokeLater(
                    new Runnable() {
                    public void run() {
                        if (tag != null) {
                            if (tag.equals("new_game")) {
                                startNewGame(game);
                            } else {
                                Card card = game.getCard(tag);
                                if (card != null) {
                                    JToggleButton button = getCardButton(card);
                                    button.setSelected(!button.isSelected());
                                    handleCards(card, button.isSelected());
                                }
                            }
                        } else {
                            speak(noInputPrompt);
                        }
                        resultsTextField.setText(text);
                        speakButton.setSelected(false);
                    }
                });
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }


    /**
     * Handle card turnovers
     *
     * @param card the card that was affected
     @ @param isSelected if <code>true</code> card was selected.
     */
    private void handleCards(Card card, boolean isSelected) {
        card.setSelected(isSelected);

        if (!isSelected) {
            game.unsetMatch(card);
        }

        updateAllCards();
        if (game.getNumSelected() >= 2) {
            if (game.processMatches()) {
                if (game.hasWon()) {
                    speak(victoryPrompt);
                } else {
                    speak(goodGuessPrompt);
                }
            } else {
                speak(badGuessPrompt);
                pauseAndTurnOver();
            }
        }
    }

    /**
     * Wait a bit, turn over the unmatched cards
     */
    private void pauseAndTurnOver() {
        Timer timer =new Timer(2000,  
                new ActionListener() {
                   public void actionPerformed(ActionEvent evt) {
                       game.turnGuessedCards();
                       updateAllCards();
                   }
                });

        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Resets the prompts
     */
    private void resetPrompts() {
        goodGuessPrompt.reset();
        badGuessPrompt.reset();
        victoryPrompt.reset();
    }
}
