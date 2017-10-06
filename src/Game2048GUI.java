/*
 * Author: David Jones
 * Date: 4/19/17
 * 
 * This class creates a JavaFx window that displays a game of 2048. The window provides buttons to load, save, display help,
 * undo a move. Allows moves to be made by pressing the arrow keys. 
 * The general layout of the window is a two HBox's and a grid pane in that order from top to bottom. 
 * The top most HBox is the scores panel which shows the current move count, score count, and highest score. 
 * The highest score is retrieved from the file "HighScore.dat"
 * The HBox below the scores panel is the command panel. The command panel contains the buttons described previously.
 * undo provide animations when pressed. The undo button is only present when an undo is possible. 
 * The buttons are made with custom regions and programmatically drawn graphics. The buttons are also hooked to the following keyboard keys
 * Alt + S = Save; Alt + L = Load; Alt + X = Exit; Alt + H = Help; Ctrl + Z = undo;
 * The grid pane contains the cell values of the game. Each cell is a custom class sized to fit the screen based on the original
 * dimension of 132x132.
 * When the user wins a dialog is displayed asking them if they want to continue. If they choose not to the game is ended
 * When the game ends either by the user's choice or because no other moves are possible then a custom pop-up dialog is displayed. 
 * If the user has made a new high score then they are given a 'tada' sound and a message for 5 seconds when the end game dialog is
 * displayed. Then the user is greeted by a "Game Over" message. If the user has lost the game due to their last move then the user
 * is asked via a dialog if they would like to undo their last move. Should they choose not to then the game window is closed and the 
 * onClosed event handler is fired if it exists
 * 
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import fxExtras.FloppyRegion;
import fxExtras.LabelInBlock;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import twentyFortyEight.Twenty48Game;

public class Game2048GUI 
{
	public Game2048GUI() throws ClassNotFoundException, IOException
	{	
		/*
		 * Constructor to load a saved game of 2048. Restores the exact state that the game was in when it was saved
		 */
		
		this(null);		//Call the base constructor with a null instance to load
	}
	
	public Game2048GUI(int numberOfRows,int numberOfColumns) throws ClassNotFoundException, IOException
	{	/*
			Creates a layout for displaying a new instance of a Twenty48Game. 
			Initializes the game according to the number of rows and columns specified
		*/
		
		this(new Twenty48Game(numberOfRows, numberOfColumns));
	}
	
	public void setOnWindowClose(EventHandler<WindowEvent> closeEventHandler)
	{
		/*
		 * Registers a single event handler to be fired when the game window closes
		 */
		
		windowCloseEventHandler = closeEventHandler;
	}
	
	public static void showHelp()
	{
		/*
		 * Display a message dialog box the explains the game to the user
		 */
		
		//Display an information alert without a header, titled "Help"
		Alert helpAlert = new Alert(AlertType.INFORMATION,"",ButtonType.CLOSE);
		helpAlert.setHeaderText(null);
		helpAlert.setTitle("Help");
		helpAlert.setContentText(
					"The goal of 2048 is to combine cells of the same value until you acheive the value of 2048. " +
					"When the game starts two random cells are spawned. " +
					"You have the option to shift all cells up, down, left or right. " +
					"Moves are made by using the arrow keys. " +
					"Once you choose a direction to move, all cells are pushed to that direction on the board. " +
					"If two cells next to each other in the direction of movement have the same value they are combined into one cell. " +
					"At the end of every move a 2 or 4 is spawned into a random cell.\n" +
					"On every move other options exist:\n" +
					"\t-To undo your last move press Ctrl + Z.\n" + 
					"\t-To display an in-game help menu press Alt + H.\n"+
					"\t-To save the current game press Alt + S.\n" +
					"\t-To load a previously saved game press Alt + L.\n" +
					"Enjoy the game and good luck!!!");
		helpAlert.show();
	}
	
	private Stage gameStage;					//The window of the game. Created when the class is constructed
	private CellPane2048 [][] gameCells;		//An array of UI elements that represent the cell values
	private Twenty48Game currentGame;			//The current instance of the 2048 game. Contains on the logic for the game
	private LabelInBlock displayedScore;		//A box that displays "SCORE" with the current score below it
	private LabelInBlock displayedHighScore;	//A box that displays "HIGH SCORE" with the current high score below it
	private LabelInBlock displayedMoveCount;	//A box that displays "MOVE COUNT" with the current move count below it
	private UndoButton undoButton;				//A button that when pressed undoes the last move. 
	private EventHandler<WindowEvent> windowCloseEventHandler;	//Handler for when the window of the game is closed
	private int loadedHighScore;				//The high score that has been loaded from the file "HighScore.dat"
	
	//If true then the user is asked if they want to undo their last move when the game ends. If false then then user is not asked.
	//This value is only true if a move resulted in the game ending
	private boolean askToUndoMove = false;				
	
	private Game2048GUI(Twenty48Game gameInstance) throws ClassNotFoundException, IOException
	{
		/*
		 * This is the base constructor for the class that should be called every time the class is constructed
		 * The constructor creates a window. If gameInstance is not null then the window is initialized to display that
		 * gameInstace. If the gameInstance is null then the user is prompted to load a saved game file. If an error occurs
		 * when loading a game the exception is thrown
		 */
		
		gameStage= new Stage();											//Create the game window;
		if(gameInstance == null) 	currentGame = loadGame(gameStage);	//Load the game since the argument is null 
		else					 	currentGame = gameInstance;			//The game will be the supplied argument
		
		/*
		 * When the window close event is fired the event is ignored. Instead a end game dialog is displayed. If the user selects
		 * to undo the move then the game resumes as normal. If the user does not select to undo the move or undoing the move is not 
		 * available then the window is closed and the event handler registered with setOnWindowClose is called if it exists
		 */
		gameStage.setOnCloseRequest(e->
		{
			
			//Show the endGame window which will show the new high score if one was made and "Game Over".
			EndGameDialog endGameWindow = new EndGameDialog();
			endGameWindow.showAndWait();
			
			//Did the user choose to NOT undo the last move? If so notify the event handler registered with setOnWindowClose 
			//and allow the window to close
			if(!endGameWindow.getResult())
			{
				if(windowCloseEventHandler != null)
					windowCloseEventHandler.handle(new WindowEvent(gameStage,WindowEvent.WINDOW_CLOSE_REQUEST));
				return;
			}
			e.consume();				//Don't close the window yet. The user decided to undo their last move
			askToUndoMove = false;		//Reset ask to undo last move
			undoButton.handle(null);	//Pretend the undo button being clicked
		});
		
		//Initialize the UI
		this.initializeGamePane();
	}
	
	private void initializeGamePane()
	{	
		/*
		 * This function initializes the UI for the game of 2048. Refer to the header comment for all the UI elements that are created
		 */
		
		//Create a UI element for the score, high score, and move count. All these elements will be a box with a set label
		//on top of modifiable label.
		this.displayedScore = new LabelInBlock("SCORE",String.valueOf(currentGame.getScore()));
		this.displayedHighScore = new LabelInBlock("HIGH SCORE",String.valueOf(loadHighScore()));
		this.displayedMoveCount = new LabelInBlock("MOVE COUNT",String.valueOf(currentGame.getMoveCount()));
		
		//Create an HBox that holds the current score, high score, and the move count.
		HBox hbScoresPanel = new HBox();
		hbScoresPanel.setStyle("-fx-alignment: center; -fx-spacing: 5;"); 							//Align center and pad controls
		hbScoresPanel.getChildren().addAll(displayedMoveCount,displayedScore,displayedHighScore);	//Add current score, high score, move count
		
		//Create a HBox that holds an Undo and Reset button. Will be aligned on the right side of the screen by adding this to the right
		//pane of a border pane
		HBox hbRightCommandPanel = new HBox();
		hbRightCommandPanel.setStyle("-fx-alignment: center-right; -fx-spacing: 5; -fx-padding: 5 0 5 0;");	//Align right and pad controls
		undoButton = currentGame.isUndoPossible() ? new UndoButton(false) : new UndoButton(true);
		hbRightCommandPanel.getChildren().addAll(undoButton);								//Add undo and reset button
		
		//Create a HBox that holds a load, save, exit and help button. Will be aligned on the left side of the screen by adding this to the
		//left pane of a border pane
		HBox hbLeftCommandPanel = new HBox();
		hbLeftCommandPanel.setStyle("-fx-alignment: center-left; -fx-spacing: 5; -fx-padding: 5 0 5 0;");	//Align left and pad controls
		hbLeftCommandPanel.getChildren().addAll(new LoadButton(),new SaveButton(), new ExitButton(), new HelpButton());
		
		//Create the cells and GridPane for the game board
		GridPane gameGrid = new GridPane();
		
		/*
		 * Calculate the cell border size. Since the game board can be made with more than just 4x4 the cellBorderSize and
		 * cellSideLength are proportional to the values of the a 4x4 cell
		 */
		double cellBorderSize = CellPane2048.calculateBorderSize(currentGame.TOTAL_ROWS,currentGame.TOTAL_COLUMNS);
		
		//Set the background color of the grid and make the corner of the outside of the grid rounded by a factor proportional to the same
		//roundness of the cells
		gameGrid.setStyle("-fx-alignment: center; -fx-background-color: gray; -fx-background-radius:" + cellBorderSize + ";");
		
		//Calculate the size the grid needs to be to fit all the cells. Set the min and max of the grid to this size so the grid
		//will not resize and change the bounds of the background
		double singleCellSideLength = CellPane2048.getCellSize(currentGame.TOTAL_ROWS, currentGame.TOTAL_COLUMNS);
		double desiredWidth = singleCellSideLength * currentGame.TOTAL_COLUMNS + 2 * cellBorderSize; //Width of all cells and the border
		double desiredHeight = singleCellSideLength * currentGame.TOTAL_ROWS + 2 * cellBorderSize;	 //Height of all cells and the border
		gameGrid.setMaxSize(desiredWidth, desiredHeight);
		gameGrid.setMinSize(desiredWidth, desiredHeight);
		
		//Initialize the game cells
		gameCells = new CellPane2048[currentGame.TOTAL_ROWS][currentGame.TOTAL_COLUMNS];
		for(int row = 0; row < currentGame.TOTAL_ROWS; row ++)
			for(int column = 0; column < currentGame.TOTAL_COLUMNS; column ++)
			{
				//Create the game cell with corresponding cell value in the currentGame
				gameCells[row][column] = new CellPane2048(currentGame.getCellValue(row, column),currentGame.TOTAL_ROWS,currentGame.TOTAL_COLUMNS);
				//Since currentGame [0,0] is bottom left the grid pane [0,0] is top left so add in the opposite order
				gameGrid.add(gameCells[row][column],column, currentGame.TOTAL_ROWS - row);
			}
		
		//Create a vbox to hold the scores, a border pane which contains the command panels, and the game grid.
		VBox vbGameBox = new VBox(hbScoresPanel,new BorderPane(null,null,hbRightCommandPanel, null, hbLeftCommandPanel),gameGrid);
		vbGameBox.setAlignment(Pos.CENTER);
		
		//Create a center HBox to hold the VBox that holds all the UI elements. This will allow for the command buttons to stay in line
		//with the game grid even when the window is resized
		HBox hbParent = new HBox();
		hbParent.getChildren().add(vbGameBox);
		hbParent.setAlignment(Pos.CENTER);
	
		//Set event handler for when a keyboard keys are pressed
		hbParent.setOnKeyPressed(e->
		{
			KeyCode code = e.getCode();
			
			//If the key pressed was an arrow key and the move was not possible then the UI does not need updated
			if		(code == KeyCode.LEFT 	&& !currentGame.moveLeft())		return;   
			else if	(code == KeyCode.RIGHT 	&& !currentGame.moveRight())	return;	 
			else if	(code == KeyCode.UP 	&& !currentGame.moveUp())		return;
			else if	(code == KeyCode.DOWN 	&& !currentGame.moveDown())		return;
			
			//Was Ctrl + Z pressed? If so then perform an undo move. The undo button updates the UI so no need to update it in this function
			if(e.isControlDown() && code == KeyCode.Z)	
			{
				undoButton.handle(null);
				return;
			}
			
			//Was Alt + S pressed to save a game? If so then prompt the user to save the game. No need to update the UI
			else if (e.isAltDown()	&& code == KeyCode.S)
			{
				this.saveGame();
				return;
			}
			
			//Was Alt + L pressed to load a game? If so then prompt the user to open a game. Update board will be called
			else if(e.isAltDown()	&& code == KeyCode.L)	openGame();
			
			//Was Alt + X pressed to exit the game? If so then fire the close window event
			else if(e.isAltDown() && code == KeyCode.X)	
			{
				gameStage.fireEvent(new WindowEvent(gameStage,WindowEvent.WINDOW_CLOSE_REQUEST));
				return;
			}
			
			//Was Alt+H pressed to display help? If so display the help window
			else if(e.isAltDown() && code == KeyCode.H)
			{
				showHelp();
				return;
			}
			
			this.updateBoard();		//Update the UI of the game
		});

		//Set the scene for the game window, show it and give focus to the top level control
		gameStage.setScene(new Scene(hbParent));
		gameStage.show();
		hbParent.requestFocus();
	}
	
	private static Twenty48Game loadGame(Window windowToShowDialog) throws ClassNotFoundException, IOException
	{	/*
			Shows the user an open file dialog and returns the Twenty48Game that is read from the file. 
			Returns null if the user cancels. If the file is corrupted the user is notified and null is returned
		*/
		
		//Prompt the user with an open file dialog and get the file they selected
		FileChooser openFileDialog = new FileChooser(); 
		openFileDialog.setTitle("Open Game");														
		openFileDialog.getExtensionFilters().add(new ExtensionFilter ("2048 Saved Game","*.dat")); 
		File gameFile = openFileDialog.showOpenDialog(windowToShowDialog);
		
		//If the user canceled the open file dialog then return null
		if(gameFile == null)		return null;
		
		//Load the saved instance state of the Twenty48Game.
		ObjectInputStream objectStream = new ObjectInputStream(new FileInputStream(gameFile));
		try
		{
			return new Twenty48Game(objectStream);
		}
		finally
		{
			objectStream.close();
		}
	}
	
	private void openGame()
	{	
		/*
		 * 	Prompts the user for a game file to open and loads the saved game and updates the scene on the current stage. If an
		 * error occurs the user is given a error dialog
		 */
		
		try
		{
			 this.currentGame = loadGame(this.gameStage); 			//Prompt the user for a file and open the saved game
		}
		catch(Exception e)
		{
			//Tell the user the loading of the saved game failed
			new Alert(AlertType.ERROR,"The saved game of 2048 is missing or corrupted",ButtonType.OK); 
			return;
		}
		
		//Update the UI
		this.initializeGamePane();	
	}
	
	private void saveGame()
	{	/*
			Prompts the user for to select a location of where to save the current game. Then attempts to save the current game
			If an error occurs then the user is notified via an error message and nothing is saved
		*/
		
		//Prompt the user to select a file to save the game
		FileChooser saveFileDialog = new FileChooser(); 
		saveFileDialog.setTitle("Save Game");														//Display a title to the user
		saveFileDialog.getExtensionFilters().add(new ExtensionFilter("2048 Saved Game","*.dat"));	//Only accept .dat files
		File gameFile = saveFileDialog.showSaveDialog(gameStage);									//Show the save file dialog to the user
		if(gameFile == null)	return;					//Did the user cancel picking a file? If so don't do anything									
		
		//Open the file selected by the user and write the state of the game to that file. If an error occurs notify the user
		try(ObjectOutputStream objectStream = new ObjectOutputStream(new FileOutputStream(gameFile)))
		{
			currentGame.serializeToStream(objectStream);
		} 
		catch (Exception e)	
		{
			new Alert(AlertType.ERROR,"The game could not be saved",ButtonType.OK).show();
		}
	}
	
	private int loadHighScore()
	{
		/*
		 * Loads the high score from the file HighScore.dat if it exists. The file just contains an integer with the high score
		 */
		loadedHighScore = 0;
		
		//If the file doesn't exist then don't try to open it
		File highScoreFile = new File("HighScore.dat");
		if(!highScoreFile.exists()) return loadedHighScore;
		
		//Read the high score from "HighScore.dat"
		try(DataInputStream inputStream = new DataInputStream(new FileInputStream(highScoreFile)))
		{
			loadedHighScore = inputStream.readInt();
		}
		catch(Exception e)
		{
			//Tell the user that the high score file could not be read
			new Alert(AlertType.ERROR,"Failed to load the high score",ButtonType.OK).show();
		}
		return loadedHighScore;
	}
	
	private void updateBoard()
	{	/*
			Updates the game layout. Updates the cell values, whether the undo button is available, high score, current score, and move count
			Checks the game status to see if the game has ended or a winner has occurred
		*/
		
		//Update the cells with their corresponding values in the game logic
		for(int column = 0; column < currentGame.TOTAL_COLUMNS; column ++)
			for(int row = 0; row < currentGame.TOTAL_ROWS; row ++)
				gameCells[row][column].updateValue(currentGame.getCellValue(row, column));
		
		//Update the score, high score, and move count
		displayedScore.setValue(String.valueOf(currentGame.getScore()));
		if(Integer.valueOf(displayedHighScore.getValue()) < currentGame.getScore())		//Only change the high score if the current score is greater
			this.displayedHighScore.setValue(String.valueOf(currentGame.getScore()));
		displayedMoveCount.setValue(String.valueOf(currentGame.getMoveCount()));
		
		//If the undo button and disabled and undo is possible then animate the undo button into view
		//If the undo button is enabled and undo is not possible then animate the undo button out of view
		//Both are achieved by toggleEnabled
		if(undoButton.isDisabled() && currentGame.isUndoPossible() || !undoButton.isDisabled() && !currentGame.isUndoPossible())  
			undoButton.toggleEnabled();
		
		//Check the status of the game
		Twenty48Game.GameStatus currentStatus = currentGame.getGameStatus();
		
		//Was the move made a winning move? If so ask user if they want to keep playing. Get the status of the game again
		//to make sure another move is possible.
		if (currentStatus == Twenty48Game.GameStatus.WIN)
		{
			//Create an information dialog asking the user if they want to continue playing. Show and wait for the user to respond
			Alert winAlert = new Alert(AlertType.INFORMATION,"Congratulations you have won!!!\nWould you like to continue playing?",
					ButtonType.YES,ButtonType.NO);
			winAlert.setHeaderText(null);
			winAlert.showAndWait();
			
			//If the user does not want to play again then fire the close window event for the game window
			if(winAlert.getResult() != ButtonType.YES)
				gameStage.fireEvent(new WindowEvent(null, WindowEvent.WINDOW_CLOSE_REQUEST));
			
			//Get status again to make sure a move is still possible. If a move is not possible then it will be checked on the next if
			//statement
			currentStatus = currentGame.getGameStatus(); 		
		}
		
		//Are no more moves available? If so inform the user and ask if the want to undo their last move.
		if(currentStatus == Twenty48Game.GameStatus.LOST || currentStatus == Twenty48Game.GameStatus.WON_BUT_UNPLAYABLE)
		{
			askToUndoMove = true;		//The only time the user is asked to undo their last move is when the last move resulted in a game ending
			//Fire the close window event for the game window. The user will be asked there if they want to undo
			gameStage.fireEvent(new WindowEvent(gameStage, WindowEvent.WINDOW_CLOSE_REQUEST));
		}
			
	}
	
	private class EndGameDialog extends Dialog<Boolean>
	{
		/*
		 * Shows an animated end game dialog. The first animation lasts 5 seconds and it is only played if a new high score is reached past
		 *	the previously saved one. Then game over is displayed for 2 seconds. If askToUndoMove is true then the user is asked
		 *	if they want to undo their last move. If they are not asked or choose not to undo their last move getResult() returns false.
		 * If they choose to do undo their last move then getResult() returns true. showAndWait() should be called before getResult() is
		 * called. Refrain from using show() because the actual value of getResult() is not correct until the dialog is exited 
		 */
		
		Rectangle backDrop = new Rectangle(400,400);	//This is the background of the dialog
		Label lblStatus = new Label("A new high score was achieved!!!");	//Displays "Game Over" or "A new high score was achieved!!!"
		Label lblNewHighScore = new Label(displayedHighScore.getValue());	//Displays the new high score. Not visible if Game Over is displayed
		
		EndGameDialog()
		{
			//Initializes the dialog for a new high score dialog. The background is blue with white text
			
			backDrop.setStyle("-fx-fill:blue; -fx-arc-height: 100; -fx-arc-width: 100;");
			lblStatus.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white; -fx-alignment: center;");
			lblNewHighScore.setStyle("-fx-font-size: 16; -fx-text-fill:white; -fx-alignment : center;"); 
			
			//Create a VBox to hold the status and new high score
			VBox vbControls = new VBox();
			vbControls.getChildren().addAll(lblStatus,lblNewHighScore);
			vbControls.setAlignment(Pos.CENTER);
			
			//Create an HBox to align the controls in the VBox in the center of the screen
			HBox vbAlignment = new HBox();
			vbAlignment.getChildren().addAll(vbControls);
			vbAlignment.setAlignment(Pos.CENTER);
			
			//Create a stack pane to hold the background and labels contained in the alignmentPane
			StackPane parentPane = new StackPane();
			parentPane.setAlignment(Pos.CENTER);
			parentPane.getChildren().addAll(backDrop,vbAlignment);
			
			//Remove any children of the dialog if any exist and set the content to the stack pane
			this.getDialogPane().getChildren().clear();
			this.getDialogPane().setContent(parentPane);
			
			//Create an animation frame to be played when the animation starts. This is the new high score animation
			//The animation will play a sound and save the new high score
			KeyFrame highScoreFrame = new KeyFrame(Duration.seconds(0),e->
			{
				//Play tada sound. If it is not found in the resources then notify the user
				try
				{
					Media tadaClip = new Media(Game2048GUI.class.getResource("tada.wav").toURI().toString());
					MediaPlayer audioPlayer = new MediaPlayer(tadaClip);
					audioPlayer.play();
				}
				catch(Exception audioException)
				{
					new Alert(AlertType.ERROR,"The tada sound file could not be played",ButtonType.OK).show();
				}
				
				//Save the new high score. If an error occurs then notify the user
				loadedHighScore = Integer.parseInt(displayedHighScore.getValue());
				try(DataOutputStream highScoreStream = new DataOutputStream(new FileOutputStream("HighScore.dat")))
				{
					highScoreStream.writeInt(loadedHighScore);
				}
				catch(Exception highScoreSaveException)
				{
					new Alert(AlertType.ERROR,"The high score could not be saved",ButtonType.OK).show();
				}
			});
			
			//Create an animation frame to be played when Game Over needs to be displayed. Since the new high score will only be displayed
			//for 5 seconds this animation starts at 5 seconds. The background is changed to red, the newHighScoreLabel is hidden
			KeyFrame endGameFrame = new KeyFrame(Duration.seconds(5),e->
			{
				lblStatus.setText("Game Over!!!");
				backDrop.setStyle("-fx-fill:blue; -fx-arc-height: 100; -fx-arc-width: 100;");
				lblNewHighScore.setVisible(false);
			});
			
			//Add the highScore animation and endGame animation to an animation timeline.
			//Also add a third animation that does nothing but allows the endGame animation to last for 2 seconds before the timeline
			//expires
			Timeline animationTimeline = new Timeline(highScoreFrame,endGameFrame,new KeyFrame(Duration.seconds(7),e->{}));
			
			//If appropriate ask the user if they want to undo their last move when the animation timeline is finished
			animationTimeline.setOnFinished(finishedHandler->
			{	
				if(!askToUndoMove)		//If we shouldn't ask to undo then set the result to false(do not undo), close the dialog and return;
				{
					this.setResult(false);
					this.close();
					return;
				}
				//Run on the UI thread(for some reason onFinished does not like showAndWait())
				Platform.runLater(()->
				{
					this.setResult(false);		//Set the result to false temporarily so this dialog can be hidden
					this.hide();				//Hide this dialog
					
					//Ask the user if they want to undo their last move
					Alert askToUndoLastMoveAlert = new Alert(AlertType.INFORMATION,"Would you like to undo your last move?",ButtonType.YES,ButtonType.NO);
					askToUndoLastMoveAlert.showAndWait();
					
					//if the user said yes they want to undo their last move then set the result of this dialog to true
					if(askToUndoLastMoveAlert.getResult() == ButtonType.YES)	this.setResult(true);
					this.close();
				});
				
			});
			
			//When the dialog is shown run the animations
			this.setOnShown(e->
			{
				//Is the current score of the game not a high score? If so skip the high score animation but jumping past it
				if(loadedHighScore >= Integer.parseInt(displayedHighScore.getValue()))
					animationTimeline.jumpTo(Duration.seconds(5));
				animationTimeline.play();
			});
		}
	}
	
	private class UndoButton extends Region implements EventHandler<MouseEvent>
	{
		/*
		 * A UI button equivalent that has a sideways U-turn symbol with an arrow at the end of one of the U's.
		 * The button spins when pressed and restores the game to the state of the last move. When an undo is not
		 * available the button disappears from sight. 
		 */
		
		public UndoButton(boolean disabled)
		{
			//Initializes the button. If disabled is true then the button is not displayed
			
			//Install the tooltip
			Tooltip.install(this,this.undoTooltip);
			this.setDisable(disabled);
			
			//Is the control to be disabled? If so do not show the control otherwise let it be shown
			if(disabled)	this.setOpacity(0);
			else			this.setOpacity(1);
			
			//Register the event handler for when it is clicked
			this.setOnMouseClicked(this);
			
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);		//Create the background 40x40 grey
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;");
			
			Arc sideArc = new Arc(23,20,5,5,-90,180);					//Create a half circle from (-90,90)
			sideArc.setStyle("-fx-stroke: white; -fx-fill: gray; -fx-stroke-width: 3;");
			
			Line bottomLine = new Line(23,25,15,25);					//Create a line for the bottom half of the circle
			bottomLine.setStyle("-fx-stroke: white; -fx-stroke-width: 3;");
			
			Line upperLine = new Line(23,15,17,15);						//Create a line for the top half of the circle
			upperLine.setStyle("-fx-stroke: white; -fx-stroke-width: 3;");
			
			//Create  triangle for the top half of the circle that is at the end of the line opposite the side of the arc
			Polygon upperTriangle = new Polygon(17,13,17,17,13,15);		
			upperTriangle.setStyle("-fx-fill: white; -fx-stroke: white;");
			
			iconGroup.getChildren().addAll(sideArc,upperTriangle,upperLine,bottomLine);
			this.getChildren().addAll(backDrop,iconGroup);
		}
		
		@Override
		public void handle(MouseEvent arg0)
		{
			if(!currentGame.undo())							return;
			this.animateClick();
			updateBoard();
		}
		
		public void toggleEnabled()
		{	
			/*
				Toggles the state of the undo button. If it is enabled it is animated out of view. If it is disabled it is enabled and
				animated into view 
			*/
			
			if(this.isDisabled())	animateIntoView();
			else					animateOutOfView();
		}
		
		private boolean isAnimating;					//If true an animation is currently being played so do not play another
		private Group iconGroup = new Group();			//The group of elements to animate
		private Tooltip undoTooltip = new Tooltip("Click to Undo Your Last Move");	//Tooltip for the button
		
		private void animateOutOfView()
		{
			//Animates the button out of view by rotating clockwise and fading
			
			//Rotate clockwise
			RotateTransition iconTransition = new RotateTransition(new javafx.util.Duration(500),iconGroup);
			iconTransition.setAxis(new Point3D(0,0,20));
			iconTransition.setFromAngle(0);
			iconTransition.setToAngle(-360);
			
			//Fade to invisible
			FadeTransition fade = new FadeTransition(new javafx.util.Duration(500),this);
			fade.setFromValue(1);
			fade.setToValue(0);
			
			//Play the animation
			fade.play();
			iconTransition.play();
			this.setDisable(true);
		}
		
		private void animateIntoView()
		{//Animates the button into view by rotating CCW and fading from invisible to visible
			
			//Rotate CCW
			RotateTransition iconTransition = new RotateTransition(new javafx.util.Duration(500),iconGroup);
			iconTransition.setAxis(new Point3D(0,0,20));
			iconTransition.setFromAngle(0);
			iconTransition.setToAngle(360);
			
			//Fade from invisible to visible
			FadeTransition fade = new FadeTransition(new javafx.util.Duration(500),this);
			fade.setFromValue(0);
			fade.setToValue(1);
			
			//Play the animation
			fade.play();
			iconTransition.play();
			this.setDisable(false);
		}
		
		private void animateClick()
		{
			/*
			 * Animation that is played when the undo button is pressed. Rotates the icon CW and enlarges then shrinks back to normal size
			 */
			
			//No need to animate is an animation is already happening
			if(isAnimating) return;
			this.isAnimating = true;		//An animation is now happening
			
			//Rotate CW
			RotateTransition iconTransition = new RotateTransition(new javafx.util.Duration(250),iconGroup);
			iconTransition.setAxis(new Point3D(0,0,20));
			iconTransition.setFromAngle(0);
			iconTransition.setToAngle(-360);
			iconTransition.setOnFinished(e->isAnimating = false);
			
			//Enlarge then shrink
			ScaleTransition onClickTransition = new ScaleTransition(new javafx.util.Duration(125),iconGroup);
			onClickTransition.setByX(1.1);
			onClickTransition.setByY(1.1);
			onClickTransition.setAutoReverse(true);
			onClickTransition.setCycleCount(2);

			//Play the animation
			onClickTransition.play();
			iconTransition.play();
		}
		
	}
	
	private class ExitButton extends Region 
	{
		/*
		 * A UI button equivalent that has a door. When pressed the game closes. 40x40 button
		 */
		
		public ExitButton()
		{	
			/*
			 * Creates the button with a white door on it
			 */
			
			
			//Create the background (white)
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;"); 
			
			//Create the door and place it right of center
			Rectangle door = new Rectangle(20,30, Color.WHITE);
			door.setX(10);
			door.setY(5);
			
			//Create the door nob and place it on the door
			Circle doorNob = new Circle(2,Color.GRAY);
			doorNob.setCenterX(13);
			doorNob.setCenterY(20);
			
			//Install the tooltip
			Tooltip.install(this, new Tooltip("Exit Game"));
			
			//When the button is clicked fire the close window event for the game window
			this.setOnMouseClicked(e->{gameStage.fireEvent(new WindowEvent(gameStage,WindowEvent.WINDOW_CLOSE_REQUEST));});
			this.getChildren().addAll(backDrop,door,doorNob);
		}
	}
	
	private class SaveButton extends Region
	{
		/*
		 * A UI button equivalent that has a blue arrow pointing to a floppy disk. When pressed the game is saved. 40x40
		 */
		
		SaveButton()
		{
			//Create the background
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;"); 
			
			//Create the floppy image and place it right of center
			FloppyRegion floppy = new FloppyRegion(Color.GRAY, Color.WHITE);
			floppy.setTranslateX(10);
			floppy.setTranslateY(5);
			
			//Create the arrow point into the floppy
			Line arrowLine = new Line(3,20,8,20);
			arrowLine.setStyle("-fx-stroke: blue; -fx-stroke-width: 5;");
			Polygon arrowHead = new Polygon(8,15,15,20,8,25);
			arrowHead.setStyle("-fx-fill: blue;");
			
			//Install the tooltip
			Tooltip.install(this, new Tooltip("Save the current game"));
			//When the button is clicked save the game
			this.setOnMouseClicked(e->saveGame());
			this.getChildren().addAll(backDrop,floppy,arrowLine,arrowHead);
		}
	}
	
	private class LoadButton extends Region
	{
		/*
		 * A UI button equivalent that has a blue arrow point out from a floppy disk. When pressed a game is loaded
		 */
		
		LoadButton()
		{
			//Create the background
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;"); 
			
			//Create a floppy image and place it right of center
			FloppyRegion floppy = new FloppyRegion(Color.GRAY, Color.WHITE);
			floppy.setTranslateX(10);
			floppy.setTranslateY(5);
			
			//Create an arrow coming out of the floppy
			Line arrowLine = new Line(12,20,8,20);
			arrowLine.setStyle("-fx-stroke: blue; -fx-stroke-width: 5;");
			Polygon arrowHead = new Polygon(8,15,1,20,8,25);
			arrowHead.setStyle("-fx-fill: blue;");
			
			//Install the tooltip
			Tooltip.install(this, new Tooltip("Load a saved game"));
			
			//When the button is pressed open a game to be loaded into the current window
			this.setOnMouseClicked(e->openGame());
			
			this.getChildren().addAll(backDrop,floppy,arrowLine,arrowHead);
		}
	}
	
	private class HelpButton extends Region
	{
		/*
		 * A UI button equivalent that has a question mark. When pressed a help alert dialog is shown.
		 */
		HelpButton()
		{
			//Create the background
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;"); 
			
			//Create a question mark and center it
			Label lblQuestionMark = new Label("?");
			lblQuestionMark.setFont(Font.font(null, FontWeight.BOLD, 22));
			lblQuestionMark.setTranslateX(16);
			lblQuestionMark.setTranslateY(4);
			lblQuestionMark.setTextFill(Color.WHITE);
			
			//When the button is clicked show the help dialog
			this.setOnMouseClicked(e->showHelp());
			this.getChildren().addAll(backDrop,lblQuestionMark);
		}
	}
	
	private static class CellPane2048 extends StackPane
	{
		/*
		 * A UI element that represents a cell of a 2048 game. The cell has a border around it 1/2 of the required space in between
		 * cells (18 pixels). This is because two cells next to each other will create the required 18 pixel border. It is a stack pane
		 * because the top most control is a label, under that is the cell , and under that is the border around the cell
		 */
		Rectangle cellRectangle;			//The background of the cell. The color changes depending on the cell value
		Label cellLabel = new Label();		//The value of the cell that is displayed
		

		public static double calculateCellSideLength(double totalRows, double totalColumns)
		{
			/*
			 * Returns the cell length that is proportional to the cell size when the game board is 4x4.
			 */
			
			//If the game board is the default value then return the default value 132
			if(totalRows == 4 && totalColumns == 4) return 132;
			
			//Calculate the max Y and X values to fit the board and all the cells on the screen
			Rectangle2D screenBounds = Screen.getPrimary().getBounds();
			double maxY = screenBounds.getHeight();
			double maxX = screenBounds.getWidth();
			
			//We don't want the game grid taking more than 5/8 of the screen vertically or horizontally
			maxY *= 2.5/(4 * totalRows);
			maxX *= 2.5/(4 * totalColumns);
			
			//Return whichever dimension is the smallest
			return (maxY < maxX) ? maxY : maxX;
		}
		
		public static double getCellSize(int totalRows, int totalColumns)
		{
			//Calculates the size of the cell including the border placed around the cell 
			
			double cellSideLength = calculateCellSideLength(totalRows, totalColumns);
			return cellSideLength + 2 * calculateBorderSize(totalRows, totalColumns);
		}
		public static double calculateArcSize(double cellSideLength)
		{
			//Calculates the arc rounding to perform in proportion to the default value (9) of a 4x4 game 
			return cellSideLength / 132 * 9;
		}
		private static double calculateFontSize(int totalRows, int totalColumns)
		{
			//Calculates the font size of the cell in proportion to the default value (42) of a 4x4 game 
			return calculateCellSideLength(totalRows, totalColumns)/132 * 42;
		}
		public static double calculateBorderSize(int totalRows,int totalColumns)
		{
			//Calculates the border size in proportion to the default value (9) of a 4x4 game 
			return calculateCellSideLength(totalRows,totalColumns)/ 132 * 9;
		}
		
		public CellPane2048(int initialValue,int totalRows, int totalColumns)
		{
			//Initialize the cell with the value and appropriate size
			
			double cellLength = calculateCellSideLength(totalRows, totalColumns);	//cellLength is the number of pixels without a border (132 by default)
			double cellBorderSize = getCellSize(totalRows, totalColumns);			//cellBorderSize is the number of pixels to surroung the cell with (9 by default)
			String cellArcSize = String.valueOf(calculateArcSize(cellLength));		//cellArcSize is the rounding of the corners (9 by default)
			
			//Create the cell and update it's value
			cellRectangle = new Rectangle(cellLength,cellLength);
			cellRectangle.setStyle("-fx-arc-height: " + cellArcSize + "; -fx-arc-width: " + cellArcSize + ";");
			cellLabel.setFont(Font.font(null, FontWeight.BOLD, calculateFontSize(totalRows, totalColumns)));
			this.updateValue(initialValue);
			
			//Create the border of the cell that is set to the background of the gridPane
			Rectangle cellBorder = new Rectangle(cellBorderSize,cellBorderSize);
			cellBorder.setFill(Color.GRAY);
			
			//Add the border, cell rectangle, and cell label to the control
			this.getChildren().addAll(cellBorder,cellRectangle,cellLabel);
		}
		public void updateValue(int value)
		{
			/*
			 * Updates the label of the cell and it's color.
			 */
			
			
			if(value != 0)	cellLabel.setText(String.valueOf(value));	//If the cell is not zero then set the label to it's value.
			else			cellLabel.setText("");						//Otherwise the cell label is nothing
			
			//The color of the cell text is black if the number is less than 8 otherwise it is white
			if(value < 8)	cellLabel.setTextFill(Color.BLACK);
			else 			cellLabel.setTextFill(Color.WHITE);
			
			//Set cell rectangle color according to it's value. If the value is greater than 2048 then it is black. If the value is 0 then
			//color the cell to blend into the background
			switch(value)
			{
			case 0: 		cellRectangle.setFill(Color.DARKGRAY);				break;
			case 2:			cellRectangle.setFill(Color.rgb(238, 228, 218));	break;
			case 4:			cellRectangle.setFill(Color.rgb(223, 201, 159)); 	break;
			case 8:			cellRectangle.setFill(Color.rgb(242, 177, 121));   	break;
			case 16:		cellRectangle.setFill(Color.rgb(245, 149, 99));   	break;
			case 32:		cellRectangle.setFill(Color.rgb(246, 124, 95));    	break;
			case 64:		cellRectangle.setFill(Color.rgb(246, 94,  59));		break;
			case 128:		cellRectangle.setFill(Color.rgb(237, 207, 114));	break;
			case 256:		cellRectangle.setFill(Color.rgb(237, 204, 97)); 	break;
			case 512:		cellRectangle.setFill(Color.rgb(237, 200, 80)); 	break;
			case 1024:		cellRectangle.setFill(Color.rgb(237, 197, 63)); 	break;
			case 2048:		cellRectangle.setFill(Color.rgb(237, 194, 46));		break; 
			default:		cellRectangle.setFill(Color.BLACK);
			}
		}
	}
	
	/* This class was removed last second
	private class ResetButton extends Region
	{
		//
		 * A UI button equivalent that has two semi-circles with arrows. 40x40 pixels. When clicked the game is reset to it's initial values. 
		 * The button spins	and fades out and back into view when clicked
		 //
		
		ResetButton()
		{
			//Create a tooltip
			Tooltip resetTooltip = new Tooltip("Click to Reset the Game");
			Tooltip.install(this, resetTooltip);
			
			//When the button is clicked reset the game, animate the button
			this.setOnMouseClicked(e->
			{
				currentGame = new Twenty48Game(currentGame.TOTAL_ROWS,currentGame.TOTAL_COLUMNS);
				animateOnClick();	
			});
			
			//Create the background for the button which is gray
			Rectangle backDrop = new Rectangle(40,40,Color.GRAY);
			backDrop.setStyle("-fx-arc-width: 10; -fx-arc-height: 10;"); 
			
			//Create the upper arc which is a white line
			Arc upperArc = new Arc(20,20,10,10,30,150);
			upperArc.setStyle("-fx-stroke: white; -fx-fill: gray; -fx-stroke-width: 3;"); 
			
			//Calculate the vertices of the top arrow. (x1,y1) and (x3,y3) will be the vertices that make a line tangential to the arc at 30*
			double x1 = 20 + 13 * Math.cos(Math.PI/6);				//X coord of 13 pixels from the center of the arc at 30*
			double y1 = 20 - 13 * Math.sin(Math.PI/6);				//Y coord of 13 pixels from the center of the arc at 30*
			double x2 = 20 + 10 * Math.cos(Math.PI/9);				//X coord of 10 pixels from the center of the arc at 18*
			double y2 =  20 - 10 * Math.sin(Math.PI/10);			//Y coord of 10 pixels from the center of the arc at 18*
			double x3 = 20 + 7 * Math.cos(Math.PI/6);				//X coord of 7 pixels from the center of the arc at  30*
			double y3 = 20 - 7 * Math.sin(Math.PI/6);				//Y coord of 7 pixels from the center of the arc at  30*
			Polygon upperTriangle = new Polygon(x1,y1,x2,y2,x3,y3);	//Create the arrow
			upperTriangle.setStyle("-fx-fill: white; -fx-stroke: white;");
			
			//Create the lower arc which is a white line
			Arc lowerArc = new Arc(20,20,10,10,0,-150);
			lowerArc.setStyle("-fx-stroke: white; -fx-fill: gray; -fx-stroke-width: 3;");
			
			//Calculate the vertices of the bottom arrow and create the triangle
			x1 = 20 + 13 * Math.cos(7 * Math.PI/6);
			y1 = 20 - 13 * Math.sin(7 * Math.PI/6);
			x2 = 20 + 10 * Math.cos(11 * Math.PI/10);
			y2 =  20 - 10 * Math.sin(11 * Math.PI/10);
			x3 = 20 + 7 * Math.cos(7 * Math.PI/6);
			y3 = 20 - 7 * Math.sin(7 * Math.PI/6);
			Polygon lowerTriangle = new Polygon(x1,y1,x2,y2,x3,y3);
			lowerTriangle.setStyle("-fx-fill: white; -fx-stroke: white;"); 
			
			iconGroup.getChildren().addAll(upperArc,upperTriangle,lowerArc,lowerTriangle);
			this.getChildren().addAll(backDrop,iconGroup);
		}
		
		private Group iconGroup = new Group();	//The group of shapes to be animated
		private boolean isAnimating;			//If true then animations are skipped. If false animations are played
		
		private void animateOnClick()
		{
			//Animates the two semi-circles by fading them out while rotating then fading back in while rotating the opposite direction
			
			//Don't animate if an animation is already playing
			if(isAnimating)	return;
			isAnimating = true;			//An animation is playing now
			
			//Rotate the semi-circles contained in the group
			RotateTransition transition = new RotateTransition(new javafx.util.Duration(500),iconGroup);
			transition.setFromAngle(0);
			transition.setToAngle(-360);
			transition.setAxis(new Point3D(0,0,20));
			
			//Fade the whole button in and out of view
			FadeTransition fade = new FadeTransition(new javafx.util.Duration(250),this);
			fade.setFromValue(1);
			fade.setToValue(0);
			fade.setAutoReverse(true);
			fade.setCycleCount(2);
			fade.setOnFinished(e->isAnimating = false);
			
			//Enlarge then shrink back to normal size
			ScaleTransition onClickTransition = new ScaleTransition(new javafx.util.Duration(100),iconGroup);
			onClickTransition.setByX(1.1);
			onClickTransition.setByY(1.1);
			onClickTransition.setAutoReverse(true);
			onClickTransition.setCycleCount(2);
			
			//Start the animations
			transition.play();
			onClickTransition.play();
			fade.play();
		}
	}
	*/
}
