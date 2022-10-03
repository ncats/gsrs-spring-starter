package ix.core.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ix.core.util.InheritanceTypeIdResolver;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@InheritanceTypeIdResolver.DefaultInstance
public class GinasProcessingMessage implements ValidationMessage {
	public enum ACTION_TYPE{IGNORE, APPLY_CHANGE, FAIL, DO_NOTHING};


	public MESSAGE_TYPE messageType= MESSAGE_TYPE.INFO;
	public ACTION_TYPE actionType= ACTION_TYPE.DO_NOTHING;

	public String messageId;
	public String message;
	public static class Link{
		public String href;
		public String text;
	}
	public boolean suggestedChange=false;
	public boolean appliedChange=false;
	public List<Link> links = new ArrayList<Link>();
	
	private boolean possibleDuplicate=false;

	public GinasProcessingMessage(){}

	public GinasProcessingMessage(MESSAGE_TYPE mtype, String msg, String msgId){
		this.messageId=msgId;
		this.messageType=mtype;
		this.message=msg;
	}
	
	public GinasProcessingMessage appliableChange(boolean b){
		this.suggestedChange=b;
		return this;
	}
	
	public String toString(){
		return messageType + ": " + messageId + " " + message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof GinasProcessingMessage)) return false;
		GinasProcessingMessage that = (GinasProcessingMessage) o;
		return messageType == that.messageType &&
				Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageType, message);
	}

	public static GinasProcessingMessage ERROR_MESSAGE(String msg){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		return new GinasProcessingMessage(MESSAGE_TYPE.ERROR,msg,msgId);
	}
	public static GinasProcessingMessage WARNING_MESSAGE(String msg){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		return new GinasProcessingMessage(MESSAGE_TYPE.WARNING,msg,msgId);
	}
	public static GinasProcessingMessage NOTICE_MESSAGE(String msg){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		return new GinasProcessingMessage(MESSAGE_TYPE.NOTICE,msg,msgId);
	}
	public static GinasProcessingMessage INFO_MESSAGE(String msg){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		return new GinasProcessingMessage(MESSAGE_TYPE.INFO,msg,msgId);
	}
	public static GinasProcessingMessage SUCCESS_MESSAGE(String msg){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		return new GinasProcessingMessage(MESSAGE_TYPE.SUCCESS,msg,msgId);
	}
	public static GinasProcessingMessage ERROR_MESSAGE(String msg, Object... args){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		msg = String.format(msg, args);
		return new GinasProcessingMessage(MESSAGE_TYPE.ERROR,msg,msgId);
	}
	public static GinasProcessingMessage WARNING_MESSAGE(String msg, Object... args){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		msg = String.format(msg, args);
		return new GinasProcessingMessage(MESSAGE_TYPE.WARNING,msg,msgId);
	}
	public static GinasProcessingMessage NOTICE_MESSAGE(String msg, Object... args){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		msg = String.format(msg, args);
		return new GinasProcessingMessage(MESSAGE_TYPE.NOTICE,msg,msgId);
	}
	public static GinasProcessingMessage INFO_MESSAGE(String msg, Object... args){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		msg = String.format(msg, args);
		return new GinasProcessingMessage(MESSAGE_TYPE.INFO,msg,msgId);
	}
	public static GinasProcessingMessage SUCCESS_MESSAGE(String msg, Object... args){
		String msgId = makeMessageId(MESSAGE_TYPE.ERROR,msg);
		msg = String.format(msg, args);
		return new GinasProcessingMessage(MESSAGE_TYPE.SUCCESS,msg,msgId);
	}
	
	public static boolean ALL_VALID(Collection<GinasProcessingMessage> messages){
		for(GinasProcessingMessage gpm:messages){
			log.info("Message: " + gpm.messageId + " " + gpm.message);
			if(gpm.isProblem()){
				return false;
			}
		}
		return true;
	}
	@JsonIgnore
	public boolean isProblem(){
		return messageType == MESSAGE_TYPE.ERROR || messageType == MESSAGE_TYPE.WARNING || messageType == MESSAGE_TYPE.NOTICE;
	}
	@JsonIgnore
	public boolean isError(){
		return messageType == MESSAGE_TYPE.ERROR;
	}


	public GinasProcessingMessage addLinks(Collection<? extends Link> links){
		this.links.addAll(links);
		return this;
	}
	public GinasProcessingMessage addLink(Link l){
		this.links.add(l);
		return this;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public String getMessage() {
		return message;
	}
	
	public GinasProcessingMessage setMessage(String s){
		this.message=s;
		return this;
	}

	@Override
	public MESSAGE_TYPE getMessageType() {
		return this.messageType;
	}
	
	public GinasProcessingMessage markPossibleDuplicate(){
		possibleDuplicate=true;
		return this;
	}

	@JsonIgnore
	public boolean isPossibleDuplicate(){
		return possibleDuplicate || !this.links.isEmpty();
	}

	public void makeError() {
		this.messageType= MESSAGE_TYPE.ERROR;
	}

	public static String makeMessageId(MESSAGE_TYPE mtype, String msg) {
		String callerClassName = new Throwable().getStackTrace()[2].getClassName();
		callerClassName = callerClassName.substring(callerClassName.lastIndexOf('.') + 1);
		return mtype.toString().substring(0,1) + String.valueOf(callerClassName.hashCode()).substring(1,4) + String.valueOf(msg.hashCode()).substring(1,4);
	}

	public boolean hasLinks() {
		return !this.links.isEmpty();
	}
}
