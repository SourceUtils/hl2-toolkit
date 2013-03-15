package com.timepath.dnd;

import com.timepath.math.Vector3;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class Entity {

    private static final Logger LOG = Logger.getLogger(Entity.class.getName());

    private int constitution;

    /**
     * @return the constitution
     */
    public int getConstitution() {
        return constitution;
    }

    /**
     * @param constitution the constitution to set
     */
    public void setConstitution(int constitution) {
        this.constitution = constitution;
    }

    public void damage(int constitution) {
        setConstitution(getConstitution() - constitution);
    }

    public void heal(int constitution) {
        setConstitution(getConstitution() + constitution);
    }

    private Vector3 position = null;

    public Vector3 getLocation() {
        return position;
    }

    public void setLocation(Vector3 v) {
        if(position == null) {
            position = new Vector3();
        }
        position.set(v);
    }

    public void move(Vector3 v) {
        position = position.add(v);
    }

    private Vector3 size = new Vector3(0.5f, 0.5f, 1.8f);

    public Vector3 getSize() {
        return size;
    }

    private String name = null;

    public String getName() {
        return name;
    }

    public void setName(String str) {
        name = str;
    }

    private Vector3 direction = new Vector3(0, -1, 0);

    public Vector3 getDirection() {
        return direction;
    }

    public void setDirection(Vector3 d) {
        direction = d;
    }

}
