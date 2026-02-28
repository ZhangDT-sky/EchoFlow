package org.echoflow.core.rag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Document {
    private String id;
    private String content;
    private Map<String,Object> metadata;

    public Document(){
        this.id = UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
    }
    public Document(String content){
        this();
        this.content=content;
    }
    public Document(String content,Map<String,Object> metadata){
        this();
        this.content = content;
        this.metadata = metadata;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id=id;
    }
    public String getContent(){
        return content;
    }

}
