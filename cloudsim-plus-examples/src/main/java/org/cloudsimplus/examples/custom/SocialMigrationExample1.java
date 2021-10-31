/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimplus.examples.custom;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigration;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationAbstract;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationBestFitStaticThreshold;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationWorstFitStaticThreshold;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerBestFit;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSocial;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.selectionpolicies.VmSelectionPolicyMinimumUtilization;
import org.cloudbus.cloudsim.user.User;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSocial;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.examples.migration.MigrationExample1;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.slametrics.SlaContract;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * An example showing how perform a manual VM migration
 * according to any desired condition, such as
 * when the simulation reaches specific times.
 *
 * <p>This is a manual migration, different when using some
 * {@link VmAllocationPolicyMigration} implementation.
 * Such a policy automatically migrates VMs based on
 * a static host CPU utilization threshold.
 * In this example, such an implementation is not used
 * and therefore, automatic VM migration based on CPU utilization threshold is not triggered.
 * If you want to implement a VM migration policy, you should
 * create a subclass from some of the existing ones.
 * Usually the starting point is the {@link VmAllocationPolicyMigrationAbstract}
 * but concrete implementations such as the {@link VmAllocationPolicyMigrationBestFitStaticThreshold}
 * can be used as an example.
 * </p>
 *
 * <p>VMs are initially placed into Hosts following the
 * {@link VmAllocationPolicyFirstFit} policy, which doesn't implement automatic VM
 * migration based on CPU utilization threshold. We call this VM as "manual migration"
 * exactly because we are not using a {@link VmAllocationPolicyMigration} implementation
 * and the migration is defined by the simulation class itself.</p>
 *
 * <p>When the simulation clock reaches a specific time,
 * an arbitrary VM is migrated to an arbitrary Host.
 * The clock advance is tracked by the {@link #clockTickListener(EventInfo)},
 * that actually fires the manual migration request.
 * </p>
 *
 * @author Manoel Campos da Silva Filho
 * @see MigrationExample1
 * @since CloudSim Plus 5.0.4
 */
public final class SocialMigrationExample1 {
    /**
     * @see Datacenter#getSchedulingInterval()
     */
    private static final int  SCHEDULING_INTERVAL = 1;
    private static final int  HOSTS = 6;
    private static final int  VMS = 6;
    private static final int  HOST_MIPS = 1000; //for each PE
    private static final int  HOST_INITIAL_PES = 5;
    private static final long HOST_RAM = 500000; //host memory (MB)
    private static final long HOST_STORAGE = 1000000; //host storage
    private static final double CLOUDLET_INITIAL_CPU_USAGE_PERCENT = 0.3;
    private static final double CLOUDLET_CPU_USAGE_INCREMENT_PER_SECOND = 0.05;

    /**
     * The time spent during VM migration depend on the
     * bandwidth of the target Host.
     * By default, a {@link Datacenter}
     * uses only 50% of the BW to migrate VMs, while the
     * remaining capacity is used for VM communication.
     * This can be changed by calling
     * {@link DatacenterSimple#setBandwidthPercentForMigration(double)}.
     *
     * <p>The 16000 Mb/s is the same as 2000 MB/s. Since just half of this capacity
     * is used for VM migration, only 1000 MB/s will be available for this process.
     * The time that takes to migrate a Vm depend on the VM RAM capacity.
     * Since VMs in this example are created with 2000 MB of RAM, any migration
     * will take 2 seconds to finish, as can be seen in the logs.
     */
    private static final long   HOST_BW = 16000L; //Mb/s

    private static final int    VM_MIPS = 1000; //for each PE
    private static final long   VM_SIZE = 1000; //image size (MB)
    private static final int    VM_RAM = 10000; //VM memory (MB)
    private static final double VM_BW = HOST_BW/(double)VMS;
    private static final int    VM_PES = 4;

    private static final long   CLOUDLET_LENGTH = 40000;
    private static final long   CLOUDLET_FILESIZE = 300;
    private static final long   CLOUDLET_OUTPUTSIZE = 300;

    /**
     * List of all created VMs.
     */
    private final List<Vm> vmList = new ArrayList<>();
    private final DatacenterBrokerSimple broker;
    private final Datacenter datacenter0;
    private SlaContract contract;

    private CloudSim simulation;
    private List<Host> hostList;
    private boolean migrationRequested;

    private static final String CUSTOMER_SLA_CONTRACT = "CustomerSLA.json";

    public static void main(String[] args) {
        new SocialMigrationExample1();
    }

    private SocialMigrationExample1(){
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        ArrayList<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
        this.contract = SlaContract.getInstance(CUSTOMER_SLA_CONTRACT);

        System.out.println("Starting " + getClass().getSimpleName());
        simulation = new CloudSim();

        User alice = new User("alice", 1);
        User bob = new User("bob", 2);
        User charlie = new User("charlie", 3);
        User diana = new User("diana", 4);
        User edward = new User("edward", 5);
        User fred = new User("fred", 6);
        User george = new User("george", 7);
        User isaac = new User("isaac", 8);

        ArrayList<User> full_user_list = new ArrayList<User>();
        full_user_list.add(alice);
        full_user_list.add(bob);
        full_user_list.add(charlie);
        full_user_list.add(diana);
        full_user_list.add(edward);
        full_user_list.add(fred);
        full_user_list.add(george);
        full_user_list.add(isaac);

        alice.make_friend(bob); //This might need to change in the future depending on friendship balance
        alice.make_friend(charlie);
        charlie.make_friend(diana);
        charlie.make_friend(edward);
        alice.make_friend(edward);

        alice.make_friend(fred);
        alice.make_friend(george);
        alice.make_friend(isaac);

        for (User u: full_user_list)
        {
            u.update_network(full_user_list);
        }

        this.datacenter0 = createSocialDatacenter(full_user_list);
        broker = new DatacenterBrokerBestFit(simulation);
        createAndSubmitVms(broker);
        cloudletList.add(createCloudlet((int) CLOUDLET_LENGTH, 4, 0, 1, alice));
        cloudletList.add(createCloudlet((int) CLOUDLET_LENGTH, 4, 0, 1, alice));
        cloudletList.add(createCloudlet((int) CLOUDLET_LENGTH, 4, 0, 2, bob));
        cloudletList.add(createCloudlet((int) CLOUDLET_LENGTH, 4, 0, 2, charlie));
        broker.submitCloudletList(cloudletList);
        simulation.addOnClockTickListener(this::clockTickListener);

        //simulation.addOnClockTickListener(this::clockTickListener);

        simulation.start();

        final List<Cloudlet> finishedList = broker.getCloudletFinishedList();
        finishedList.sort(
            Comparator.comparingLong((Cloudlet c) -> c.getVm().getHost().getId())
                      .thenComparingLong(c -> c.getVm().getId()));
        new CloudletsTableBuilder(finishedList).build();
        System.out.printf("%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");

        hostList.forEach(this::printHostHistory);
        System.out.println(getClass().getSimpleName() + " finished!");
    }

    /**
     * Event listener which is called every time the simulation clock advances.
     * When the simulation clock reaches 10 seconds, it migrates an arbitrary VM to
     * an arbitrary Host.
     *
     * @param info information about the event happened.
     * @see CloudSim#addOnClockTickListener(EventListener)
     */
    private void clockTickListener(EventInfo info) {
        /*
        if(!migrationRequested && info.getTime() >= 10){
            Vm sourceVm = vmList.get(0);
            Host targetHost = hostList.get(hostList.size() - 1);
            System.out.printf("%n# Requesting the migration of %s to %s%n%n", sourceVm, targetHost);
            datacenter0.requestVmMigration(sourceVm, targetHost);
            this.migrationRequested = true;
        }*/
        VmAllocationPolicyMigration allocationPolicyNew
            = new VmAllocationPolicyMigrationWorstFitStaticThreshold(
            new VmSelectionPolicyMinimumUtilization(),
            0.7);

        allocationPolicyNew.setUnderUtilizationThreshold(this.contract.getCpuUtilizationMetric().getMinDimension().getValue());

        if(info.getTime() >= 10){
            ((DatacenterSimple)datacenter0).setVmAllocationPolicy(allocationPolicyNew);
        }
    }

    /**
     * Prints the state of a Host along the simulation time.
     * <p>Realize that the Host State History is just collected
     * if {@link Host#isStateHistoryEnabled() history is enabled}
     * by calling {@link Host#enableStateHistory()}.</p>
     *
     * @param host
     */
    private void printHostHistory(Host host) {
        new HostHistoryTableBuilder(host).setTitle(host.toString()).build();
    }

    /**
     * Creates a Cloudlet.
     *
     * @return the created Cloudlets
     */
    public Cloudlet createCloudlet(int length, int pesNumber, int social_id, int security_level, User owner) {
        final Cloudlet cloudlet =
            new CloudletSocial(length, pesNumber, this.createUtilizationModel(0.10,0.9,true), social_id, security_level, owner);
        return cloudlet;
    }

    public void createAndSubmitVms(DatacenterBroker broker) {
        final List<Vm> list = new ArrayList<>(VMS);
        for(int i = 0; i < VMS; i++){
            list.add(createVm(VM_PES));
        }

        vmList.addAll(list);
        broker.submitVmList(list);
    }

    public Vm createVm(int pes) {
        final Vm vm = new VmSocial(VM_MIPS, pes);
        vm
          .setRam(VM_RAM).setBw((long)VM_BW).setSize(VM_SIZE)
          .setCloudletScheduler(new CloudletSchedulerSpaceShared());
        return vm;
    }

    /**
     * Creates a Datacenter with number of Hosts defined by {@link #HOSTS},
     * but only some of these Hosts will be active (powered on) initially.
     *
     * @return
     */
    private Datacenter createSocialDatacenter(List<User> listOfUsers) {
        this.hostList = new ArrayList<>();
        for(int i = 0; i < listOfUsers.size(); i++){
            final int pes = HOST_INITIAL_PES + i;
            Host temp = createHost(pes, HOST_MIPS, listOfUsers.get(i));
            hostList.add(temp);
        }
        System.out.println();


        VmAllocationPolicyMigration allocationPolicy
            = new VmAllocationPolicyMigrationWorstFitStaticThreshold(
            new VmSelectionPolicyMinimumUtilization(),
            this.contract.getCpuUtilizationMetric().getMaxDimension().getValue());

        allocationPolicy.setUnderUtilizationThreshold(this.contract.getCpuUtilizationMetric().getMinDimension().getValue());

        Datacenter dc = new DatacenterSimple(simulation, hostList, allocationPolicy);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    public Host createHost(int numberOfPes, long mipsByPe, User owner) {
            List<Pe> peList = createPeList(numberOfPes, mipsByPe);
            HostSocial host = new HostSocial(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
            host.owner = owner;
            host.setVmScheduler(new VmSchedulerSpaceShared());
            host.enableStateHistory();
            return host;
    }

    public List<Pe> createPeList(int numberOfPEs, long mips) {
        final List<Pe> list = new ArrayList<>(numberOfPEs);
        for(int i = 0; i < numberOfPEs; i++) {
            list.add(new PeSimple(mips));
        }

        return list;
    }

    private UtilizationModelDynamic createUtilizationModel(
        double initialCpuUsagePercent,
        double maxCloudletCpuUsagePercent,
        final boolean progressiveCpuUsage)
    {
        initialCpuUsagePercent = Math.min(initialCpuUsagePercent, 1);
        maxCloudletCpuUsagePercent = Math.min(maxCloudletCpuUsagePercent, 1);
        final UtilizationModelDynamic um = new UtilizationModelDynamic(initialCpuUsagePercent);

        if (progressiveCpuUsage) {
            um.setUtilizationUpdateFunction(this::getCpuUtilizationIncrement);
        }

        um.setMaxResourceUtilization(maxCloudletCpuUsagePercent);
        return um;
    }

    private double getCpuUtilizationIncrement(final UtilizationModelDynamic um) {
        return um.getUtilization() + um.getTimeSpan() * CLOUDLET_CPU_USAGE_INCREMENT_PER_SECOND;
    }

}
