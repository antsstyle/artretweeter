/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;


/**
 *
 * @author antss
 */
public class OrderedByRetweetsStatusHolder extends OrderedStatusHolder {

    public OrderedByRetweetsStatusHolder(Integer likes, Integer retweets, StatusJSON status) {
        super(likes, retweets, status);
    }

    @Override
    public int compareTo(OrderedStatusHolder o) {
        return this.getRetweets().compareTo(o.getRetweets());
    }

}
