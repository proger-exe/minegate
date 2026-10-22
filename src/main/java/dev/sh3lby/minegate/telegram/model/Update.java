package dev.sh3lby.minegate.telegram.model;

public class Update {
    @com.google.gson.annotations.SerializedName("update_id")
    private Long updateId;
    private Message message;
    @com.google.gson.annotations.SerializedName("edited_message")
    private Message editedMessage;

    public Long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Message getEditedMessage() {
        return editedMessage;
    }

    public void setEditedMessage(Message editedMessage) {
        this.editedMessage = editedMessage;
    }
}
