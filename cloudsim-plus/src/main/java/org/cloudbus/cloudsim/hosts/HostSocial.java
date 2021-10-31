package org.cloudbus.cloudsim.hosts;

import org.cloudbus.cloudsim.resources.HarddriveStorage;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.user.User;

import java.util.List;

public class HostSocial extends HostSimple{
    public User owner;

    public HostSocial(final long ram, final long bw, final long storage, final List<Pe> peList) {
        super(ram, bw, new HarddriveStorage(storage), peList);
    }

    public int getOwnerSocialCredit(){
        return this.owner.social_credit;
    }

}
