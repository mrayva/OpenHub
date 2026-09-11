

package com.thirtydegreesray.openhub.ui.adapter.base;

/**
 * Created by ThirtyDegreesRay on 2017/10/18 16:03:04
 */

public class DoubleTypesModel<M1, M2> {

    private M1 m1;
    private M2 m2;

    /**
     * Only meaningful for M1 (header) entries in NotificationsAdapter: true
     * once every notification under that repo has been marked read via the
     * header's button. Drives the button's icon (single check -> double
     * check) and, on a second tap while true, removes the whole group - see
     * NotificationsPresenter.markRepoNotificationsAsRead()/removeRepoNotifications().
     */
    private boolean allRead = false;

    public DoubleTypesModel(M1 m1, M2 m2) {
        this.m1 = m1;
        this.m2 = m2;
    }

    public M1 getM1() {
        return m1;
    }

    public M2 getM2() {
        return m2;
    }

    public boolean isAllRead() {
        return allRead;
    }

    public void setAllRead(boolean allRead) {
        this.allRead = allRead;
    }

    public int getTypePosition(){
        if(m1 != null){
            return 0;
        } else if(m2 != null){
            return 1;
        } else {
            return 0;
        }
    }

}
