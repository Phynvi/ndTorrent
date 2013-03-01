package com.ndtorrent.gui;

import java.awt.EventQueue;

import javax.swing.JFrame;

import com.ndtorrent.client.Client;
import com.ndtorrent.client.status.ConnectionInfo;
import com.ndtorrent.client.status.PieceInfo;
import com.ndtorrent.client.status.StatusObserver;
import com.ndtorrent.client.status.TorrentInfo;
import com.ndtorrent.client.status.TrackerInfo;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JSplitPane;

public class Frontend implements StatusObserver {

	private JFrame frmNdtorrentAlpha;

	private Client client = new Client();
	private TableFrame trackersFrame;
	private TableFrame piecesFrame;
	private TableFrame connectionsFrame;
	private BarRenderer torrentBar;
	private JSplitPane splitPane_1;
	private JSplitPane splitPane_2;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Frontend window = new Frontend();
					window.frmNdtorrentAlpha.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Frontend() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmNdtorrentAlpha = new JFrame();
		frmNdtorrentAlpha.setTitle("ndTorrent - alpha version");
		frmNdtorrentAlpha.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frmNdtorrentAlpha.setVisible(false);
				client.close();
			}
		});
		frmNdtorrentAlpha.setBounds(100, 100, 402, 318);
		frmNdtorrentAlpha.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmNdtorrentAlpha.getContentPane().setLayout(
				new BoxLayout(frmNdtorrentAlpha.getContentPane(),
						BoxLayout.Y_AXIS));

		torrentBar = new BarRenderer();
		frmNdtorrentAlpha.getContentPane().add(torrentBar);

		splitPane_1 = new JSplitPane();
		frmNdtorrentAlpha.getContentPane().add(splitPane_1);
		splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);

		trackersFrame = new TableFrame("Trackers");
		splitPane_1.setLeftComponent(trackersFrame);
		trackersFrame.setTableModel(new TrackersModel());

		splitPane_2 = new JSplitPane();
		splitPane_2.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane_1.setRightComponent(splitPane_2);

		piecesFrame = new TableFrame("Pieces");
		splitPane_2.setLeftComponent(piecesFrame);
		piecesFrame.setTableModel(new PiecesModel());

		connectionsFrame = new TableFrame("Connections");
		splitPane_2.setRightComponent(connectionsFrame);
		connectionsFrame.setTableModel(new ConnectionsModel());

		client.setServerPort(Client.DEFAULT_PORT);

		String info_hash = client.addTorrent("test.torrent");
		client.addStatusObserver(this, info_hash);

	}

	@Override
	public void asyncConnections(final List<ConnectionInfo> connnections,
			String info_hash) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// if (!observing(info_hash)) return;
				((ConnectionsModel) connectionsFrame.getTableModel())
						.setConnections(connnections);
			}
		});
	}

	@Override
	public void asyncPieces(final List<PieceInfo> pieces, String info_hash) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// if (!observing(info_hash)) return;
				((PiecesModel) piecesFrame.getTableModel()).setPieces(pieces);
			}
		});
	}

	@Override
	public void asyncTrackers(final List<TrackerInfo> trackers, String info_hash) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// if (!observing(info_hash)) return;
				((TrackersModel) trackersFrame.getTableModel())
						.setTrackers(trackers);
			}
		});
	}

	@Override
	public void asyncTorrentStatus(final TorrentInfo torrent, String info_hash) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// ? check info_hash
				torrentBar.setBits(torrent.getBitfield(), torrent.numOfPieces());
			}
		});
	}

}
