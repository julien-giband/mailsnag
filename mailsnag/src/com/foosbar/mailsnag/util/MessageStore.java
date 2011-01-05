package com.foosbar.mailsnag.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.UUID;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.foosbar.mailsnag.Activator;
import com.foosbar.mailsnag.model.Message;
import com.foosbar.mailsnag.model.MessageData;
import com.foosbar.mailsnag.model.MessageParser;
import com.foosbar.mailsnag.model.Message.Attachment;

public class MessageStore {

	// Save message to directory
	//    workspace/.metadata/.plugins/com.foosbar.mailsnag
	public static final Message persist(String data) {

		//Parse Message
		Message message = MessageParser.parse(data, getRandomFilename());
		
		//Set message as unread
		message.setUnread(true);
		
		//Writer writer = null;
		BufferedOutputStream out = null;
		try {
				
			//Create file
			File file = new File(Activator.getDefault().getStateLocation().toFile(), message.getFilename());

			//Write file
			if(file != null) {
				out = new BufferedOutputStream(new FileOutputStream(file));
				out.write(data.getBytes());
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(out != null)
					out.close();
			} catch(Exception e) {}
		}
		
		return message;
	}

	// Save message to directory
	//    workspace/.metadata/.plugins/com.foosbar.mailsnag
	public static final void persistAttachment(Attachment attachment, BodyPart part) {
		
		Message message = attachment.getMessage();
		
		try {
			
			File root_dir = new File(Activator.getDefault().getStateLocation().toFile(), message.getAttachmentDir());
			
			if(!root_dir.exists())
				root_dir.mkdir();
			
			File dir = new File(root_dir, attachment.getId());
			
			if(!dir.exists())
				dir.mkdir();
					
			File file = new File(dir, attachment.getName());
			if(!file.exists()) {
				InputStream in = null;
				OutputStream out = null;
	
				try {
					in  = new BufferedInputStream(part.getInputStream());
					out = new BufferedOutputStream(new FileOutputStream(file));
					byte[] buffer = new byte[4096];
					while(in.read(buffer) != -1)
						out.write(buffer);
							
				} catch(Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if(out != null)
							out.close();
					} catch(Exception e) {}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	// Save message to directory
	//    workspace/.metadata/.plugins/com.foosbar.mailsnag
	public static final void persistAttachment(Attachment attachment, String data) {
		
		Message message = attachment.getMessage();
		
		InputStream is = null;
		
		try {

			Session session = Session.getDefaultInstance(new Properties());
			
			is = new ByteArrayInputStream(data.getBytes());
			
			MimeMessage mimeMessage = new MimeMessage(session, is);			
		
			if(!(mimeMessage.getContent() instanceof Multipart))
				return;
			
			MimeMultipart content = (MimeMultipart)mimeMessage.getContent();
			int count = content.getCount();
			
			File root_dir = new File(Activator.getDefault().getStateLocation().toFile(), message.getAttachmentDir());
			
			if(!root_dir.exists())
				root_dir.mkdir();
			
			File dir = new File(root_dir, attachment.getId());
			
			int counter = 0;
			for(int x = 0; x < count; x++) {
				BodyPart part = content.getBodyPart(x);
				
				if(BodyPart.ATTACHMENT.equals(part.getDisposition())) {

					if(attachment.getIndex() != counter++)
						continue;
					
					if(!dir.exists())
						dir.mkdir();
					
					File file = new File(dir, attachment.getName());
					if(!file.exists()) {
						InputStream in = null;
						OutputStream out = null;
	
						try {
							in  = new BufferedInputStream(part.getInputStream());
							out = new BufferedOutputStream(new FileOutputStream(file));
							byte[] buffer = new byte[4096];
							while(in.read(buffer) != -1)
								out.write(buffer);
							
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							try {
								if(out != null)
									out.close();
							} catch(Exception e) {}
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(is != null)
					is.close();
			} catch(Exception e) { }
		}
	}
	
	public static File createTempHtmlFile(String html) {
		
		BufferedOutputStream out = null;
		try {
			File file = File.createTempFile("email",".html");
			file.deleteOnExit();

			out = new BufferedOutputStream(new FileOutputStream(file));
			out.write(html.getBytes());

			return file;

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(out != null)
					out.close();
			} catch(Exception e) {}
		}
		return null;
	}
	
	public static final void delete(Message message) {
		try {
			File dir = Activator.getDefault().getStateLocation().toFile();
			
			File file = new File(dir, message.getFilename());

			if(file.exists()) {
				String attachmentDir = file.getName().substring(0, file.getName().length()-4);
				File aDir = new File(dir, attachmentDir);
				if(aDir.exists()) {
					File[] attachments = aDir.listFiles();
					for(File attachment : attachments)
						attachment.delete();
					aDir.delete();
				}

				file.delete();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
		}
	}
	
	public static final Message load(String filename) {
		
		Reader reader = null;
		
		try {

			StringBuilder builder = new StringBuilder();
			
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(Activator.getDefault()
							.getStateLocation().toFile(), filename))));
			
			char[] buffer = new char[8192];
			while (reader.read(buffer) > 0)
	            builder.append(buffer);
			
	        Message message = MessageParser.parse(builder.toString(), filename);
	        
	        return message;
	        
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(reader != null)
					reader.close();
			} catch(Exception e) { }
		}
		
		return null;
	}
	
	public static final MessageData loadData(Message message) {
		return MessageParser.parseData(getData(message));
	}

	public static final String getData(Message message) {
		
		Reader reader = null;
		
		try {

			StringBuilder builder = new StringBuilder();
			
			String filename = message.getFilename();
	        reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Activator.getDefault().getStateLocation().toFile(), filename))));
			char[] buffer = new char[8192];
	        while (reader.read(buffer) > 0)
	            builder.append(buffer);
			
	        return builder.toString();
	        
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(reader != null)
					reader.close();
			} catch(Exception e) { }
		}
		
		return null;
	}
	
	public static final void removeAll() {
		File dir = Activator.getDefault().getStateLocation().toFile();
		if(dir.isDirectory()) {
			File[] files = dir.listFiles(new EmailFilenameFilter());
			for(File file : files) {
				String attachmentDir = file.getName().substring(0, file.getName().length()-4);
				File aDir = new File(dir, attachmentDir);
				if(aDir.exists()) {
					File[] attachments = aDir.listFiles();
					for(File attachment : attachments)
						attachment.delete();
					aDir.delete();
				}
				file.delete();
			}
		}
	}
	
	public static final String getRandomFilename() {
		return UUID.randomUUID().toString() + Message.EXTENSION;
	}
}