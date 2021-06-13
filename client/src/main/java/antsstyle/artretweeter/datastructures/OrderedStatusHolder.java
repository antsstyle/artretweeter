/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.datastructures;


/**
 *
 * @author antss
 */
public abstract class OrderedStatusHolder implements Comparable<OrderedStatusHolder> {

    private StatusJSON status;
    private Integer likes;
    private Integer retweets;
    
    public OrderedStatusHolder(Integer likes, Integer retweets, StatusJSON status) {
        this.likes = likes;
        this.retweets = retweets;
        this.status = status;
    }

    public StatusJSON getStatus() {
        return status;
    }

    public OrderedStatusHolder setStatus(StatusJSON status) {
        this.status = status;
        return this;
    }

    public Integer getLikes() {
        return likes;
    }

    public OrderedStatusHolder setLikes(Integer likes) {
        this.likes = likes;
        return this;
    }

    public Integer getRetweets() {
        return retweets;
    }

    public OrderedStatusHolder setRetweets(Integer retweets) {
        this.retweets = retweets;
        return this;
    }

}
