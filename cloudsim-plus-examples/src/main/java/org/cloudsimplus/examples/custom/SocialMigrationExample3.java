/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
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

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationBestFitSocial;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationBestFitStaticThreshold;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationRoundRobinSocial;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigrationStaticThreshold;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerFirstFitSocial;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.cloudlets.CloudletSocial;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.power.PowerMeasurement;
import org.cloudbus.cloudsim.power.PowerMeter;
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.selectionpolicies.VmSelectionPolicyMinimumUtilization;
import org.cloudbus.cloudsim.user.User;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSocial;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.HostHistoryTableBuilder;
import org.cloudsimplus.listeners.DatacenterBrokerEventInfo;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.VmHostEventInfo;
import org.cloudsimplus.util.Log;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static java.util.Comparator.comparingLong;

/**
 * An example showing how to create 1 Datacenter having: 5 hosts
 * with increasing number of PEs (starting at 4 PEs for the 1st host);
 * 3 VMs with 2 PEs each one;
 * and 1 cloudlet by VM, each one having the same number of PEs from its VM.
 *
 *
 * <p>The example then performs VM migration using
 * a {@link VmAllocationPolicyMigrationBestFitStaticThreshold}.
 * Such a policy migrates VMs based on
 * a static host CPU utilization threshold.
 * The VmAllocationPolicy used in this example ignores power consumption of Hosts.
 * This way, it isn't required to set a PowerModel for Hosts.</p>
 *
 * <p>According to the allocation policy, VM 0 will be allocated to Host 0.
 * Since Host 0 has just 4 PEs, allocating a second VM into it
 * would cause overload.
 * Each cloudlet will start using 80% of its VM CPU.
 * As the VM 0 will run one Cloudlet and requires just 2 PEs from Host 0 (which has 4 PEs),
 * the initial Host CPU usage will be just 40% (1 VM using 80% of 2 PEs from a total of 4 Host PEs = 0.8*2 / 4).
 *
 * Allocating a second VM into Host 0 would double the Host CPU utilization,
 * overreaching its upper utilization threshold (defined as 70%).
 * This way, VMs 1 and 2 are allocated to Host 1 which has 5 PEs.
 * </p>
 *
 * <p>The {@link VmAllocationPolicyMigrationBestFitStaticThreshold}
 * allows setting static under/over CPU utilization thresholds to
 * enable VM migration.
 * The example uses a {@link UtilizationModelDynamic} to define that CPU usage of cloudlets
 * increases along simulation time.
 * The first 2 Cloudlets all start with a usage of 80% of CPU,
 * which increases along the time (see {@link #CLOUDLET_CPU_INCREMENT_PER_SECOND}).
 * The third Cloudlet starts at a lower CPU usage and increases in the same way.
 * </p>
 *
 * <p>Some constants are used to create simulation objects such as
 * {@link  DatacenterSimple}, {@link  Host} and {@link  Vm}.
 * The values of these constants were careful and accordingly chosen to allow:
 * (i) migration of VMs due to either under and overloaded hosts; and (ii)
 * the researcher to know exactly how the simulation will run
 * and what will be the final results.
 * </p>
 *
 * <p>
 * Several values impact simulation results, such as
 * (i) hosts CPU capacity and number of PEs,
 * (ii) VMs and cloudlets requirements and
 * (iii) even VM bandwidth (which defines the VM migration time).
 *
 * This way, if you want to change these values, you must
 * define new appropriated ones to allow the simulation
 * to run correctly.</p>
 *
 * <p>Realize that Host State History is just collected
 * if you enable that by calling {@link Host#enableStateHistory()}.</p>
 *
 * @author Manoel Campos da Silva Filho
 *
 * TODO Verify if inter-datacenter VM migration is working by default using the DatacenterBroker class.
 */
public final class SocialMigrationExample3 {
    /**
     * @see Datacenter#getSchedulingInterval()
     */
    private static final int  SCHEDULING_INTERVAL = 1;

    /**
     * The percentage of host CPU usage that trigger VM migration
     * due to under utilization (in scale from 0 to 1, where 1 is 100%).
     */
    private static final double HOST_UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.1;

    /**
     * The percentage of host CPU usage that trigger VM migration
     * due to over utilization (in scale from 0 to 1, where 1 is 100%).
     */
    private static final double HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION = 0.7;

    /** @see Datacenter#setHostSearchRetryDelay(double) */
    private static final int HOST_SEARCH_RETRY_DELAY = 60;

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
    private static final long   HOST_BW = 16_000L; //Mb/s

    private static final int    HOST_MIPS = 1000; //for each PE
    private static final long   HOST_RAM[] = {15_000, 500_000, 25_000,15_000, 500_000, 25_000, 15_000, 500_000, 25_000, 15_000}; //host memory (MB)
    private static final long   HOST_STORAGE = 1_000_000; //host storage

    /**
     * An array where each item defines the number of PEs for each Host to be created.
     * The length of the array represents the number of Hosts.
     */
    private static final int    HOST_PES[] = {4, 5, 5, 4, 5, 5, 4, 5, 5, 4};

    private static final int    VM_PES[]   = {2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 2, 2 , 2, 2, 2, 1};
    private static final int    VM_MIPS = 1000; //for each PE
    private static final long   VM_SIZE = 1000; //image size (MB)
    private static final int    VM_RAM = 10_000; //VM memory (MB)
    private static final double VM_BW = HOST_BW/(double)VM_PES.length;

    private static final long   CLOUDLET_LENGTH = 20_000;
    private static final long   CLOUDLET_FILESIZE = 300;
    private static final long   CLOUDLET_OUTPUTSIZE = 300;
    private static final long   CLOUDLETS = 100;

    /**
     * The percentage of CPU that a cloudlet will use when
     * it starts executing (in scale from 0 to 1, where 1 is 100%).
     * For each cloudlet create, this value is used
     * as a base to define CPU usage.
     * @see #createAndSubmitCloudlets(DatacenterBroker)
     */
    private static final double CLOUDLET_INITIAL_CPU_PERCENTAGE = 0.8;

    /**
     * Defines the speed (in percentage) that CPU usage of a cloudlet
     * will increase during the simulation execution.
     * (in scale from 0 to 1, where 1 is 100%).
     * @see #createCpuUtilizationModel(double, double)
     */
    private static final double CLOUDLET_CPU_INCREMENT_PER_SECOND = 0.04;

    /**
     * List of all created VMs.
     */
    private final List<Vm> vmList = new ArrayList<>();
    private final List<PowerMeter> meterList = new ArrayList<>();
    private final DatacenterBrokerFirstFitSocial broker;

    private final CloudSim simulation;
    private VmAllocationPolicyMigrationStaticThreshold allocationPolicy;
    private List<Host> hostList;
    private int migrationsNumber = 0;
    private final ContinuousDistribution random;
    private Random random2;

    private ArrayList<User> full_user_list;
    private int[][] edge_list;

    public ArrayList<Cloudlet> cloudletList;

    public static void main(String[] args) {
        new SocialMigrationExample3();
    }

    public static double calculateSD(double numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/(length-1));
    }



    private void generateUsers(){
        edge_list = new int[60][2];
        Random rand = new Random(127);
        for(int i=0;i<60;i++){
            int x = rand.nextInt(20);
            int y = rand.nextInt(20);
            if(x == y){
                y = rand.nextInt(20);
            }
            edge_list[i][0] = x;
            edge_list[i][1] = y;
        }

        String[] name_list = {
            "angela", "bob", "charlie", "diana",
            "edward", "fred", "george", "isaac",
            "jack", "keter", "laurent", "mark",
            "naimon", "opus", "peter", "qinxing",
            "roland", "sam", "tiffany", "umpqua"
        };
        full_user_list = new ArrayList<User>();
        for(int i =0; i<name_list.length; i++)
        {
            full_user_list.add(new User(name_list[i], i));
        }
        for(int[] j : edge_list)
        {
            full_user_list.get(j[0]).make_friend(full_user_list.get(j[1]));
        }
        full_user_list.get(0).make_friend(full_user_list.get(1));
        full_user_list.get(0).make_friend(full_user_list.get(2));
        //full_user_list.get(0).make_friend(full_user_list.get(3));


        for (User u: full_user_list)
        {
            u.update_network(full_user_list);
            //System.out.println(u.adjacency_map);
        }
    }

    private SocialMigrationExample3(){
        Log.setLevel(Level.INFO);

        generateUsers();

        if(HOST_PES.length != HOST_RAM.length){
            throw new IllegalStateException("The length of arrays HOST_PES and HOST_RAM must match.");
        }

        random = new UniformDistr();
        random2 = new Random(127);
        System.out.println("Starting " + getClass().getSimpleName());
        simulation = new CloudSim();
        cloudletList = new ArrayList<>();

        @SuppressWarnings("unused")
        final Datacenter datacenter0 = createDatacenter();
        for (Host h: hostList){
            PowerMeter meter = new PowerMeter(this.simulation, h);
            meter.setMeasurementInterval(1.0);
            meterList.add(meter);
        }
        broker = new DatacenterBrokerFirstFitSocial(simulation);
        createAndSubmitVms(broker);
        createAndSubmitOneCloudlet(full_user_list.get(0), 1);
        createAndSubmitOneCloudlet(full_user_list.get(1), 2);
        createAndSubmitOneCloudlet(full_user_list.get(2), 1);
        createAndSubmitOneCloudlet(full_user_list.get(3), 2);
        createAndSubmitOneCloudlet(full_user_list.get(4), 2);
        createAndSubmitOneCloudlet(full_user_list.get(5), 1);
        createAndSubmitOneCloudlet(full_user_list.get(6), 1);
        createAndSubmitOneCloudlet(full_user_list.get(7), 2);



        broker.addOnVmsCreatedListener(this::onVmsCreatedListener);

        simulation.start();

        final List<Cloudlet> finishedList = broker.getCloudletFinishedList();
        final Comparator<Cloudlet> cloudletComparator =
            comparingLong((Cloudlet c) -> c.getVm().getHost().getId())
                .thenComparingLong(c -> c.getVm().getId());
        finishedList.sort(cloudletComparator);
        new CloudletsTableBuilder(finishedList).build();

        System.out.printf("%nHosts CPU usage History (when the allocated MIPS is lower than the requested, it is due to VM migration overhead)%n");
        hostList.stream().filter(h -> h.getId() <= 2).forEach(this::printHostStateHistory);

        System.out.printf("%nHosts Power Consumption Total%n");

        double[] powerList = new double[10];

        for (int i=0;i<8;i++){
            var temp = meterList.get(i);
            var tempList = temp.getPowerMeasurements();
            PowerMeasurement pm= new PowerMeasurement(0,0);
            for (PowerMeasurement px: tempList){
                pm = pm.add(px);
            }
            System.out.println("Host "+i + " : " + pm.getTotalPower()/full_user_list.get(i).cloudlet_count);
            powerList[i] = pm.getTotalPower()/full_user_list.get(i).cloudlet_count;
        }
        System.out.println("Standard Deviation: "+calculateSD(powerList));

        System.out.printf("Number of VM migrations: %d%n", migrationsNumber);
        System.out.println(getClass().getSimpleName() + " finished!");
    }

    /**
     * A listener method that is called when a VM migration starts.
     * @param info information about the happened event
     *
     * @see #createAndSubmitVms(DatacenterBroker)
     * @see Vm#addOnMigrationFinishListener(EventListener)
     */
    private void startMigration(final VmHostEventInfo info) {
        final Vm vm = info.getVm();
        final Host targetHost = info.getHost();
        System.out.printf(
            "# %.2f: %s started migrating to %s (you can perform any operation you want here)%n",
            info.getTime(), vm, targetHost);
        showVmAllocatedMips(vm, targetHost, info.getTime());
        //VM current host (source)
        showHostAllocatedMips(info.getTime(), vm.getHost());
        //Migration host (target)
        showHostAllocatedMips(info.getTime(), targetHost);
        System.out.println();

        migrationsNumber++;
        if(migrationsNumber > 1){
            return;
        }

        //After the first VM starts being migrated, tracks some metrics along simulation time
        simulation.addOnClockTickListener(clock -> {
            if (clock.getTime() <= 2 || (clock.getTime() >= 11 && clock.getTime() <= 15))
                showVmAllocatedMips(vm, targetHost, clock.getTime());
        });
    }

    private void showVmAllocatedMips(final Vm vm, final Host targetHost, final double time) {
        final String msg = String.format("# %.2f: %s in %s: total allocated", time, vm, targetHost);
        final MipsShare allocatedMips = targetHost.getVmScheduler().getAllocatedMips(vm);
        final String msg2 = allocatedMips.totalMips() == VM_MIPS * 0.9 ? " - reduction due to migration overhead" : "";
        System.out.printf("%s %.0f MIPs (divided by %d PEs)%s\n", msg, allocatedMips.totalMips(), allocatedMips.pes(), msg2);
    }

    /**
     * A listener method that is called when a VM migration finishes.
     * @param info information about the happened event
     *
     * @see #createAndSubmitVms(DatacenterBroker)
     * @see Vm#addOnMigrationStartListener(EventListener)
     */
    private void finishMigration(final VmHostEventInfo info) {
        final Host host = info.getHost();
        System.out.printf(
            "# %.2f: %s finished migrating to %s (you can perform any operation you want here)%n",
            info.getTime(), info.getVm(), host);
        System.out.print("\t\t");
        showHostAllocatedMips(info.getTime(), hostList.get(1));
        System.out.print("\t\t");
        showHostAllocatedMips(info.getTime(), host);
    }

    private void showHostAllocatedMips(final double time, final Host host) {
        System.out.printf(
            "%.2f: %s allocated %.2f MIPS from %.2f total capacity%n",
            time, host, host.getTotalAllocatedMips(), host.getTotalMipsCapacity());
    }

    private void printHostStateHistory(final Host host) {
        new HostHistoryTableBuilder(host).setTitle(host.toString()).build();
    }

    private void printHostTotalPower(final Host host) {
        System.out.println("Host " + host.getId() + " : " + host.getPowerModel().getShutDownPower());
    }


    public void createAndSubmitCloudlets(DatacenterBroker broker) {
        final List<Cloudlet> list = new ArrayList<>(VM_PES.length);
        Cloudlet cloudlet = Cloudlet.NULL;
        UtilizationModelDynamic um = createCpuUtilizationModel(CLOUDLET_INITIAL_CPU_PERCENTAGE, 1);
        var cloudlet1 = createCloudlet(vmList.get(0), broker, um, full_user_list.get(0));
        ((CloudletSocial)cloudlet1).securityLevel = 1;
        list.add(cloudlet1);
        var cloudlet2 = createCloudlet(vmList.get(1), broker, um, full_user_list.get(0));
        ((CloudletSocial)cloudlet2).securityLevel = 2;
        list.add(cloudlet2);
        var cloudlet3 = createCloudlet(vmList.get(2), broker, um, full_user_list.get(2));
        ((CloudletSocial)cloudlet3).securityLevel = 2;
        list.add(cloudlet3);
        var cloudlet4 = createCloudlet(vmList.get(3), broker, um, full_user_list.get(2));
        cloudlet4.setUtilizationModelCpu(createCpuUtilizationModel(0.2, 1));
        ((CloudletSocial)cloudlet4).securityLevel = 2;
        list.add(cloudlet4);
        for(Vm vm: vmList){

        }
        cloudlet.setUtilizationModelCpu(createCpuUtilizationModel(0.2, 1));
        broker.submitCloudletList(list);

    }


    private void createAndSubmitOneCloudlet(User owned, int securityLevel) {
        final int id = cloudletList.size();
        final long length = 10000; //in number of Million Instructions (MI)
        final int pesNumber = 1;
        final UtilizationModel utilizationModelFull = new UtilizationModelFull();
        UtilizationModelDynamic um = createCpuUtilizationModel(CLOUDLET_INITIAL_CPU_PERCENTAGE, 1);

        Cloudlet cloudlet = new CloudletSocial(CLOUDLET_LENGTH, 2, owned)
            .setFileSize(CLOUDLET_FILESIZE)
            .setOutputSize(CLOUDLET_OUTPUTSIZE)
            .setUtilizationModelRam(utilizationModelFull)
            .setUtilizationModelBw(utilizationModelFull)
            .setUtilizationModelCpu(um);

        ((CloudletSocial)cloudlet).securityLevel = securityLevel;

        cloudletList.add(cloudlet);

        if(cloudletList.size() < CLOUDLETS){
            cloudlet.addOnFinishListener(info -> {
                System.out.printf("\t# %.2f: Requesting creation of new Cloudlet after %s finishes executing.%n", info.getTime(), info.getCloudlet());
                createAndSubmitOneCloudlet(owned, random2.nextInt(3));
            });
        }
        cloudlet.addOnFinishListener(credit -> {
            ((HostSocial)(cloudlet.getVm().getHost())).owner.social_credit += 1;
            ((CloudletSocial) cloudlet).owner.social_credit -= 1;
        });
        broker.submitCloudlet(cloudlet);
        ((CloudletSocial)cloudlet).owner.cloudlet_count += 1;
    }

    /**
     * Creates a Cloudlet.
     *
     * @param vm the VM that will run the Cloudlets
     * @param broker the broker that the created Cloudlets belong to
     * @param cpuUtilizationModel the CPU UtilizationModel for the Cloudlet
     * @return the created Cloudlets
     */
    public Cloudlet createCloudlet(Vm vm, DatacenterBroker broker, UtilizationModel cpuUtilizationModel, User owned) {
        final UtilizationModel utilizationModelFull = new UtilizationModelFull();

        final Cloudlet cloudlet =
            new CloudletSocial(CLOUDLET_LENGTH, (int)vm.getNumberOfPes(), owned)
                .setFileSize(CLOUDLET_FILESIZE)
                .setOutputSize(CLOUDLET_OUTPUTSIZE)
                .setUtilizationModelRam(utilizationModelFull)
                .setUtilizationModelBw(utilizationModelFull)
                .setUtilizationModelCpu(cpuUtilizationModel);
        broker.bindCloudletToVm(cloudlet, vm);

        return cloudlet;
    }

    public void createAndSubmitVms(DatacenterBroker broker) {
        final List<Vm> list = new ArrayList<>(VM_PES.length);
        for (final int pes : VM_PES) {
            list.add(createVm(pes));
        }

        vmList.addAll(list);
        broker.submitVmList(list);

        list.forEach(vm -> vm.addOnMigrationStartListener(this::startMigration));
        list.forEach(vm -> vm.addOnMigrationFinishListener(this::finishMigration));
    }

    public Vm createVm(final int pes) {
        Vm vm = new VmSocial(VM_MIPS, pes);
        vm
            .setRam(VM_RAM).setBw((long)VM_BW).setSize(VM_SIZE)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());
        return vm;
    }

    /**
     * Creates a CPU UtilizationModel for a Cloudlet.
     * If the initial usage is lower than the max usage, the usage will
     * be dynamically incremented along the time, according to the
     * {@link #getCpuUsageIncrement(UtilizationModelDynamic)}
     * function. Otherwise, the CPU usage will be static, according to the
     * defined initial usage.
     *
     * @param initialCpuUsagePercent the percentage of CPU utilization
     * that created Cloudlets will use when they start to execute.
     * If this value is greater than 1 (100%), it will be changed to 1.
     * @param maxCpuUsagePercentage the maximum percentage of
     * CPU utilization that created Cloudlets are allowed to use.
     * If this value is greater than 1 (100%), it will be changed to 1.
     * It must be equal or greater than the initial CPU usage.
     * @return
     */
    private UtilizationModelDynamic createCpuUtilizationModel(double initialCpuUsagePercent, double maxCpuUsagePercentage) {
        if(maxCpuUsagePercentage < initialCpuUsagePercent){
            throw new IllegalArgumentException("Max CPU usage must be equal or greater than the initial CPU usage.");
        }

        initialCpuUsagePercent = Math.min(initialCpuUsagePercent, 1);
        maxCpuUsagePercentage = Math.min(maxCpuUsagePercentage, 1);
        final UtilizationModelDynamic um;
        if (initialCpuUsagePercent < maxCpuUsagePercentage) {
            um = new UtilizationModelDynamic(initialCpuUsagePercent)
                .setUtilizationUpdateFunction(this::getCpuUsageIncrement);
        }
        else um = new UtilizationModelDynamic(initialCpuUsagePercent);

        um.setMaxResourceUtilization(maxCpuUsagePercentage);
        return um;
    }

    /**
     * Increments the CPU resource utilization, that is defined in percentage values.
     * @return the new resource utilization after the increment
     */
    private double getCpuUsageIncrement(final UtilizationModelDynamic um){
        return um.getUtilization() + um.getTimeSpan()*CLOUDLET_CPU_INCREMENT_PER_SECOND;
    }

    /**
     * Creates a Datacenter with number of Hosts defined by the length of {@link #HOST_PES},
     * but only some of these Hosts will be active (powered on) initially.
     *
     * @return
     */
    private Datacenter createDatacenter() {
        this.hostList = createHosts();
        System.out.println();

        /**
         * Sets an upper utilization threshold higher than the
         * {@link #HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION}
         * to enable placing VMs which will use more CPU than
         * defined by the value in the mentioned constant.
         * After VMs are all submitted to Hosts, the threshold is changed
         * to the value of the constant.
         * This is used to  place VMs into a Host which will
         * become overloaded in order to trigger the migration.
         */
        this.allocationPolicy =
            new VmAllocationPolicyMigrationRoundRobinSocial(
                new VmSelectionPolicyMinimumUtilization(),
                HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION + 0.2);
        this.allocationPolicy.setUnderUtilizationThreshold(HOST_UNDER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);

        final Datacenter dc = new DatacenterSimple(simulation, hostList, allocationPolicy);
        for (Host host : hostList) {
            System.out.printf(
                "# Created %s with %.0f MIPS x %d PEs (%.0f total MIPS)%n",
                host, host.getMips(), host.getNumberOfPes(), host.getTotalMipsCapacity());
        }
        dc.setSchedulingInterval(SCHEDULING_INTERVAL)
          .setHostSearchRetryDelay(HOST_SEARCH_RETRY_DELAY);
        return dc;
    }

    private List<Host> createHosts() {
        final List<Host> list = new ArrayList<>(HOST_PES.length);
        for (int i = 0; i < HOST_PES.length; i++) {
            final int pes = HOST_PES[i];
            final long ram = HOST_RAM[i];
            Host temp = createHost(pes, ram);
            temp.setPowerModel(new PowerModelHostSimple(140.0, 1.0));
            ((HostSocial)temp).owner = full_user_list.get(i);
            //System.out.println(full_user_list.get(i).username);
            list.add(temp);



        }

        return list;
    }

    public Host createHost(final int pesNumber, final long ram) {
        final List<Pe> peList = createPeList(pesNumber);
        final Host host = new HostSocial(ram, HOST_BW, HOST_STORAGE, peList);
        host.setVmScheduler(new VmSchedulerTimeShared());
        host.enableStateHistory();
        return host;
    }

    public List<Pe> createPeList(final int pesNumber) {
        final List<Pe> list = new ArrayList<>(pesNumber);
        for(int i = 0; i < pesNumber; i++) {
            list.add(new PeSimple(HOST_MIPS));
        }

        return list;
    }

    /**
     * A listener that is called after all VMs from a broker are created,
     * setting the allocation policy to the default value
     * so that some Hosts will be overloaded with the placed VMs and migration will be fired.
     *
     * The listener is removed after finishing, so that it's called just once,
     * even if new VMs are submitted and created latter on.
     */
    private void onVmsCreatedListener(final DatacenterBrokerEventInfo info) {
        System.out.printf("# All %d VMs submitted to the broker have been created.%n", broker.getVmCreatedList().size());
        allocationPolicy.setOverUtilizationThreshold(HOST_OVER_UTILIZATION_THRESHOLD_FOR_VM_MIGRATION);
        broker.removeOnVmsCreatedListener(info.getListener());
        vmList.forEach(vm -> showVmAllocatedMips(vm, vm.getHost(), info.getTime()));

        System.out.println();
        hostList.forEach(host -> showHostAllocatedMips(info.getTime(), host));
        System.out.println();
    }

}