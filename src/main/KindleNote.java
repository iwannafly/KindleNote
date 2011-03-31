/*

    Copyright (c) 2011 by Stanislav (proDOOMman) Kosolapov <prodoomman@gmail.com>

 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************
*/

package main;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazon.kindle.kindlet.AbstractKindlet;
import com.amazon.kindle.kindlet.KindletContext;
import com.amazon.kindle.kindlet.event.KindleKeyCodes;
import com.amazon.kindle.kindlet.ui.KBoxLayout;
import com.amazon.kindle.kindlet.ui.KLabel;
import com.amazon.kindle.kindlet.ui.KLabelMultiline;
import com.amazon.kindle.kindlet.ui.KMenu;
import com.amazon.kindle.kindlet.ui.KMenuItem;
import com.amazon.kindle.kindlet.ui.KOptionPane;
import com.amazon.kindle.kindlet.ui.KPagedContainer;
import com.amazon.kindle.kindlet.ui.KOptionPane.ConfirmDialogListener;
import com.amazon.kindle.kindlet.ui.KOptionPane.InputDialogListener;
import com.amazon.kindle.kindlet.ui.KOptionPane.MessageDialogListener;
import com.amazon.kindle.kindlet.ui.KPages;
import com.amazon.kindle.kindlet.ui.KTextArea;
import com.amazon.kindle.kindlet.ui.KTextField;
import com.amazon.kindle.kindlet.ui.KTextOptionOrientationMenu;
import com.amazon.kindle.kindlet.ui.KTextOptionPane;
import com.amazon.kindle.kindlet.ui.border.KLineBorder;
import com.amazon.kindle.kindlet.ui.pages.PageProviders;

public class KindleNote extends AbstractKindlet {

	private KindletContext ctx;
	private KPages homeMenu;
	private KLabelMultiline plainText;
	private KTextArea textEdit;
	private String currentFileName;
	private KMenu menu;
	private KMenuItem newItem;
	private String file2del;
	private KFakeMenuItem item2del;
	private KFakeMenuItem lastFocus;
	private KTextField searchField;
//	private KFakeMenuItem[] itemsList;
	private List itemsList;
	private boolean textIsNew = false;
	private KLabel pageLabel;
	private KFakeMenuItem item2rename;
	
	public void create(KindletContext context) {
		this.itemsList = new ArrayList();
		this.ctx = context;
		this.pageLabel = new KLabel("KindleNote by proDOOMman",KLabel.CENTER);
		this.pageLabel.setForeground(Color.white);
		this.pageLabel.setBackground(Color.black);
		ctx.getRootContainer().add(pageLabel, BorderLayout.SOUTH);
		this.menu = new KMenu();
		this.searchField = new KTextField();
		this.searchField.setBorder(new KLineBorder(3,true));
		this.searchField.addTextListener(new TextListener(){
			public void textValueChanged(TextEvent arg0) {
				String tofind = searchField.getText();

				ListIterator iterator = itemsList.listIterator(itemsList.size());
				homeMenu.removeAllItems();
				List tempItemsList = new ArrayList();
				while (iterator.hasPrevious()) {
					KFakeMenuItem element = (KFakeMenuItem)iterator.previous();
					String text = element.getText();
					boolean found = true;
					if(text.indexOf(tofind)==-1)
						found = false;
					if(found)
						tempItemsList.add(element);
				}
				Object[] tempItems = tempItemsList.toArray();
				Arrays.sort(tempItems, new Comparator() {
					public int compare(Object arg0, Object arg1) {
						KFakeMenuItem o1 = (KFakeMenuItem)arg0;
						File f1 = new File(ctx.getHomeDirectory()+"/"+o1.getText()+".txt");
						KFakeMenuItem o2 = (KFakeMenuItem)arg1;
						File f2 = new File(ctx.getHomeDirectory()+"/"+o2.getText()+".txt");
						return f1.lastModified() > f2.lastModified() ? -1 :
							f1.lastModified() == f2.lastModified() ? 0 :
								1;
					}
				});
				for(int i = 0; i<tempItems.length; i++)
					homeMenu.addItem((KFakeMenuItem)tempItems[i]);
				homeMenu.repaint();
			}
		});
		ctx.getRootContainer().add(searchField, BorderLayout.NORTH);
		this.newItem = new KMenuItem("Новая заметка");
		this.newItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Date dtn = new Date();
			    SimpleDateFormat formatter1 = new SimpleDateFormat(
			        "dd.MM.yyyy hh-mm");
			    String dt=formatter1.format(dtn);
				KOptionPane.showInputDialog(ctx.getRootContainer(),
						"Название заметки: ", dt,new InputDialogListener() {

					public void onClose(String arg0) {
						File file = new File(ctx.getHomeDirectory(),arg0+".txt");
						if(!file.exists())
						{
							try {
								file.createNewFile();
								addHomeItem(arg0,0);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						openAndEditFile(arg0);
					}
				});
			}
		});
		this.menu.add(newItem);
		this.menu.add("О программе", new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					KOptionPane.showMessageDialog(ctx.getRootContainer(),
							"Программа KindleNote\nCopyleft proDOOMman 2011\nLicense: GPLv3\n" +
							"Комбинации клавиш:\n" +
							"Del - удалить заметку\n" +
							"Enter - открыть заметку (первое нажатие - чтение, второе - редактирование)\n" +
							"R - переименовать заметку\n" +
							"E - редактировать заметку\n" +
							"Back - возврат в предыдущее меню",
							new MessageDialogListener(){
								public void onClose() {
									//nothing
								}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		this.ctx.setMenu(this.menu);
		this.textEdit = new KTextArea();
		this.textEdit.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					arg0.consume();
				}
			}
			public void keyReleased(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					if(textIsNew)
					{
						try {
							FileWriter outFile = new FileWriter(currentFileName);
							PrintWriter out = new PrintWriter(outFile);
							out.print(textEdit.getText());
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						plainText.setText(textEdit.getText());
						homeMenu.removeItem(lastFocus);
						homeMenu.addItem(lastFocus, 0);
						textIsNew = false;
					}
					else if(plainText.getText().compareTo(textEdit.getText())!=0)
					{
						KOptionPane.showConfirmDialog(ctx.getRootContainer(), "Сохранить изменения?", new ConfirmDialogListener(){
							public void onClose(int arg0) {
								if(arg0 == KOptionPane.OK_OPTION)
								{
									try {
										FileWriter outFile = new FileWriter(currentFileName);
										PrintWriter out = new PrintWriter(outFile);
										out.print(textEdit.getText());
										out.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
									plainText.setText(textEdit.getText());
									homeMenu.removeItem(lastFocus);
									homeMenu.addItem(lastFocus, 0);
								}
							}
						});
					}
					ctx.getRootContainer().remove(textEdit);
					ctx.getRootContainer().add(plainText);
					plainText.requestFocus();
					arg0.consume();
				}
				else if(arg0.getKeyCode() == KindleKeyCodes.VK_FIVE_WAY_SELECT)
				{
					//выделение текста
				}
			}
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					arg0.consume();
				}
			}
		});
		this.plainText = new KLabelMultiline();
		this.plainText.setFocusable(true);
		this.plainText.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					arg0.consume();
				}
			}
			public void keyReleased(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					ctx.getRootContainer().remove(plainText);
					ctx.getRootContainer().add(homeMenu);
					ctx.getRootContainer().add(searchField, BorderLayout.NORTH);
					ctx.getRootContainer().add(pageLabel, BorderLayout.SOUTH);
					ctx.setSubTitle("");
					currentFileName = "";
					newItem.setEnabled(true);
					homeMenu.requestFocus();
					lastFocus.requestFocus();
					arg0.consume();
				}
				else// if(arg0.getKeyCode() == KindleKeyCodes.VK_FIVE_WAY_SELECT)
				{
					ctx.getRootContainer().remove(plainText);
					textEdit.setText(plainText.getText());
					ctx.getRootContainer().add(textEdit);
					textEdit.requestFocus();
				}
			}
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KindleKeyCodes.VK_BACK)
				{
					arg0.consume();
				}
			}
		});
	}

	public void start() {
		new Controller(ctx.getHomeDirectory().getAbsolutePath()); // physkeybru
		try {
			ctx.getProgressIndicator().setIndeterminate(true);
			ctx.getProgressIndicator().setString("Запуск...");
			KTextOptionPane pane = new KTextOptionPane();
			pane.addOrientationMenu(new KTextOptionOrientationMenu());
			ctx.setTextOptionPane(pane);
			File f = ctx.getHomeDirectory();
			File[] files = f.listFiles(new FilenameFilter() {
				
				public boolean accept(File arg0, String arg1) {
					if(arg1.endsWith(".txt") &&
							!arg1.endsWith("keyboard.txt") &&
							!arg1.endsWith("keyboard_european.txt"))
						return true;
					return false;
				}
			});
			Arrays.sort(files, new Comparator() {
				public int compare(Object arg0, Object arg1) {
					File o1 = (File)arg0;
					File o2 = (File)arg1;
					return o1.lastModified() > o2.lastModified() ? -1 :
						o1.lastModified() == o2.lastModified() ? 0 :
							1;
				}
			});
			
			this.homeMenu = new KPages(PageProviders.createKBoxLayoutProvider(KBoxLayout.PAGE_AXIS));
			this.homeMenu.setPageKeyPolicy(KPagedContainer.PAGE_KEYS_GLOBAL);
			this.homeMenu.setFirstPageAlignment(BorderLayout.NORTH);
			for(int i = 0; i<files.length; i++)
			{
				String noteName = files[i].getName().substring(0, files[i].getName().length()-4);
				addHomeItem(noteName);
			}
			ctx.getRootContainer().add(this.homeMenu);
			ctx.getProgressIndicator().setIndeterminate(false);
		} catch (Throwable t) {
			t.printStackTrace();
			ctx.setSubTitle("Error in constructor!");
			ctx.getRootContainer().removeAll();
			ctx.getRootContainer().add(new KLabelMultiline(t.getLocalizedMessage()));
			ctx.getRootContainer().repaint();
		}
	}
	public void addHomeItem(String itemName, int position)
	{
		KFakeMenuItem item = new KFakeMenuItem(itemName);
		item.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				String fname = e.getActionCommand().substring(1,e.getActionCommand().length());
				if(e.getActionCommand().startsWith("E")) //открыть на чтение
				{
					openFile(fname);
					lastFocus = (KFakeMenuItem)e.getSource();
				}
				else if(e.getActionCommand().startsWith("W"))//Открыть на запись
				{
					openAndEditFile(fname);
					lastFocus = (KFakeMenuItem)e.getSource();
				}
				else if(e.getActionCommand().startsWith("R"))//Переименовать
				{
					item2rename = (KFakeMenuItem)e.getSource();
					KOptionPane.showInputDialog(ctx.getRootContainer(), "Новое название заметки: ", item2rename.getText(), new InputDialogListener() {
						public void onClose(String arg0) {
							File f = new File(ctx.getHomeDirectory(),item2rename.getText()+".txt");
							if(f.renameTo(new File(ctx.getHomeDirectory(),arg0+".txt")))
								item2rename.setMText(arg0);
							item2rename.requestFocus();
							item2rename = null;
						}
					});
				}
				else if(e.getActionCommand().startsWith("D"))
				{
					file2del = fname;
					item2del = (KFakeMenuItem)e.getSource();
					KOptionPane.showConfirmDialog(null, "Удалить заметку \""+fname+"\"?",
							new ConfirmDialogListener(){
						public void onClose(int arg0) {
							if(arg0 == KOptionPane.OK_OPTION)
							{
								File f = new File(ctx.getHomeDirectory().getAbsolutePath()+"/"+file2del+".txt");
								f.delete();
								homeMenu.removeItem(item2del);
								itemsList.remove(item2del);
								item2del = null;
								file2del = "";
							}
						}
					});
				}
				else if(e.getActionCommand().startsWith("F1"))
				{
					pageLabel.setText("Заметка "+String.valueOf(homeMenu.indexOfItem(e.getSource())+1)
							+" из "+
							String.valueOf(itemsList.size()));
					pageLabel.repaint();
				}
				else if(e.getActionCommand().startsWith("F0"))
				{
					pageLabel.setText("");
					pageLabel.repaint();
				}
			}
		});
		item.setBorder(new KLineBorder(4,true));
		if(position == -1)
			this.homeMenu.addItem(item);
		else
		{
			lastFocus = item;
			this.homeMenu.addItem(item, position);
		}
		itemsList.add(item);
	}
	public void addHomeItem(String itemName)
	{
		addHomeItem(itemName, -1);
	}
	public void openFile(String filename)
	{
			currentFileName = ctx.getHomeDirectory().getAbsolutePath()+"/"+filename+".txt";
			String text = new String();
		    String str;
			try {
				BufferedReader in = new BufferedReader(new FileReader(currentFileName));
				try {
					while ((str = in.readLine()) != null) {
						text = text + str + "\n";
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			    try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				KOptionPane.showMessageDialog(null, "Файл не найден!", new MessageDialogListener() {
					
					public void onClose() {
						//nothing
					}
				});
				e.printStackTrace();
				return;
			}
			this.newItem.setEnabled(false);
			ctx.getRootContainer().remove(this.homeMenu);
			ctx.getRootContainer().remove(this.searchField);
			ctx.getRootContainer().remove(this.pageLabel);
			plainText.setText(text);
			ctx.getRootContainer().add(plainText);
			plainText.requestFocus();
			ctx.setSubTitle(filename);
	}
	public void openAndEditFile(String filename)
	{
		textIsNew = true;
		openFile(filename);
		ctx.getRootContainer().remove(plainText);
		textEdit.setText(plainText.getText());
		ctx.getRootContainer().add(textEdit);
		textEdit.requestFocus();
	}
}