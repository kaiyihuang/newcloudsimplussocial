package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSocial;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.user.User;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSocial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatacenterBrokerFirstFitSocial extends DatacenterBrokerSimple{

    private int lastVmIndex;
    /**
     * Creates a DatacenterBroker object.
     *
     * @param simulation The CloudSim instance that represents the simulation the Entity is related to
     */
    public DatacenterBrokerFirstFitSocial(final CloudSim simulation) {
        super(simulation);
    }

    @Override
    public Vm defaultVmMapper(final Cloudlet cloudlet)
    {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }

        ArrayList<Vm> temp_vm_list = new ArrayList<Vm>();

        for(Vm vm_choice: getVmCreatedList())
        {
            HashMap<User, Integer> myadjacency_list = ((CloudletSocial)cloudlet).owner.adjacency_map;
            if( myadjacency_list.get( ((HostSocial)(vm_choice.getHost())).owner) == ((CloudletSocial)cloudlet).securityLevel ) {
                temp_vm_list.add(vm_choice);
            }
        }
        for(Vm vm_choice: getVmCreatedList())
        {
            HashMap<User, Integer> myadjacency_list = ((CloudletSocial)cloudlet).owner.adjacency_map;
            if( myadjacency_list.get( ((HostSocial)(vm_choice.getHost())).owner) < ((CloudletSocial)cloudlet).securityLevel && myadjacency_list.get( ((HostSocial)(vm_choice.getHost())).owner) != 0) {
                temp_vm_list.add(vm_choice);
            }
        }
        for(Vm vm_choice: getVmCreatedList())
        {
            HashMap<User, Integer> myadjacency_list = ((CloudletSocial)cloudlet).owner.adjacency_map;
            if( myadjacency_list.get( ((HostSocial)(vm_choice.getHost())).owner) == 0) {
                temp_vm_list.add(vm_choice);
            }
        }
        //Collections.reverse(temp_vm_list);
        System.out.println(temp_vm_list);

        final int maxTries = temp_vm_list.size();
        for (int i = 0; i < maxTries; i++) {
            final Vm vm;
            if(temp_vm_list.size() <= lastVmIndex)
                vm = temp_vm_list.get(temp_vm_list.size()-1);
                else
                    vm = temp_vm_list.get(lastVmIndex);
            if (vm.getExpectedFreePesNumber() >= cloudlet.getNumberOfPes()) {
                LOGGER.trace("{}: {}: {} (PEs: {}) mapped to {} (available PEs: {}, tot PEs: {})",
                    getSimulation().clockStr(), getName(), cloudlet, cloudlet.getNumberOfPes(), vm,
                    vm.getExpectedFreePesNumber(), vm.getFreePesNumber());
                return vm;
            }

            /* If it gets here, the previous Vm doesn't have capacity to place the Cloudlet.
             * Then, moves to the next Vm.
             * If the end of the Vm list is reached, starts from the beginning,
             * until the max number of tries.*/
            lastVmIndex = ++lastVmIndex % temp_vm_list.size();
        }

        LOGGER.warn("{}: {}: {} (PEs: {}) couldn't be mapped to any suitable VM.",
            getSimulation().clockStr(), getName(), cloudlet, cloudlet.getNumberOfPes());

        return Vm.NULL;


    }

    @Override
    public boolean bindCloudletToVm(final Cloudlet cloudlet, final Vm vm) {
        if (!this.equals(cloudlet.getBroker())) {
            return false;
        }
        ((VmSocial)vm).owner = ((CloudletSocial)cloudlet).owner;
        ((VmSocial)vm).securityLevel = ((CloudletSocial)cloudlet).securityLevel;
        cloudlet.setVm(vm);


        return true;
    }



}
