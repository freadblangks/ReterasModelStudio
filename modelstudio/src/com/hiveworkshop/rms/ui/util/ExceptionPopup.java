package com.hiveworkshop.rms.ui.util;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class ExceptionPopup {
	public static void display(final Throwable e) {

		final JTextPane pane = new JTextPane();

		final OutputStream stream = new OutputStream() {
			public void updateStreamWith(final String s) {
				final Document doc = pane.getDocument();
				try {
					doc.insertString(doc.getLength(), s, null);
				} catch (final BadLocationException e) {
					JOptionPane.showMessageDialog(null,
							"MDL open error popup failed to create info popup.");
					e.printStackTrace();
				}
			}

			@Override
			public void write(final int b) {
				updateStreamWith(String.valueOf((char) b));
			}

			@Override
			public void write(final byte[] b, final int off, final int len) {
				updateStreamWith(new String(b, off, len));
			}

			@Override
			public void write(final byte[] b) {
				write(b, 0, b.length);
			}
		};
		final PrintStream ps = new PrintStream(stream);
		ps.println("Unknown error occurred:");
		e.printStackTrace(ps);

		// Make the exception popup not huge and scrollable
		JScrollPane jScrollPane = new JScrollPane(pane);
		jScrollPane.setPreferredSize(new Dimension(800, 800));
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		JOptionPane.showMessageDialog(null, jScrollPane);
	}

	public static void display(final String s, final Exception e) {

		final JTextPane pane = new JTextPane();

		final OutputStream stream = new OutputStream() {
			public void updateStreamWith(final String s) {
				final Document doc = pane.getDocument();
				try {
					doc.insertString(doc.getLength(), s, null);
				} catch (final BadLocationException e) {
					JOptionPane.showMessageDialog(null, "MDL open error popup failed to create info popup.");
					e.printStackTrace();
				}
			}

			@Override
			public void write(final int b) throws IOException {
				updateStreamWith(String.valueOf((char) b));
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				updateStreamWith(new String(b, off, len));
			}

			@Override
			public void write(final byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		final PrintStream ps = new PrintStream(stream);
		ps.println(s + ":");
		e.printStackTrace(ps);

		// Make the exception popup not huge and scrollable
		JScrollPane jScrollPane = new JScrollPane(pane);
		jScrollPane.setPreferredSize(new Dimension(800, 800));
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		JOptionPane.showMessageDialog(null, jScrollPane);
	}
}
