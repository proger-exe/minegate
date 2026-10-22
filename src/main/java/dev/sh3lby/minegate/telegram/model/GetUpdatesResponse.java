package dev.sh3lby.minegate.telegram.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GetUpdatesResponse {
    private Boolean ok;
    private List<Update> result;

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public List<Update> getResult() {
        return result;
    }

    public void setResult(List<Update> result) {
        this.result = result;
    }
}
