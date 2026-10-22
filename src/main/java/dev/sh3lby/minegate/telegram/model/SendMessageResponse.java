package dev.sh3lby.minegate.telegram.model;

public class SendMessageResponse {
    private Boolean ok;
    private Message result;

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public Message getResult() {
        return result;
    }

    public void setResult(Message result) {
        this.result = result;
    }
}
