package org.cloudbus.cloudsim.vms;

import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.user.User;

public class VmSocial extends VmSimple{
    public User owner;
    public int securityLevel;

    public boolean verifyHostSecurity(HostSocial h)
    {
        return this.owner.adjacency_map.get(h.owner) <= this.securityLevel;
    }

    public VmSocial(final Vm sourceVm){
        super(sourceVm);
    }

    public VmSocial(final double mipsCapacity, final long numberOfPes)
    {
        super(mipsCapacity, numberOfPes);
    }

    public VmSocial(final long id, final long mipsCapacity, final long numberOfPes) {
        super(id,mipsCapacity,numberOfPes);
    }


}
