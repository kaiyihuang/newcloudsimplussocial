package org.cloudbus.cloudsim.allocationpolicies.migration;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.selectionpolicies.VmSelectionPolicy;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSocial;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class VmAllocationPolicyMigrationRoundRobinSocial extends VmAllocationPolicyMigrationStaticThreshold{
    /**
     * Creates a VmAllocationPolicyMigrationBestFitStaticThreshold.
     * It uses a {@link #DEF_OVER_UTILIZATION_THRESHOLD default over utilization threshold}
     * and a {@link #DEF_UNDERLOAD_THRESHOLD default under utilization threshold}.
     *
     * @param vmSelectionPolicy the policy that defines how VMs are selected for migration
     * @see #setUnderUtilizationThreshold(double)
     * @see #setOverUtilizationThreshold(double)
     */
    public VmAllocationPolicyMigrationRoundRobinSocial(final VmSelectionPolicy vmSelectionPolicy) {
        this(vmSelectionPolicy, DEF_OVER_UTILIZATION_THRESHOLD);
    }

    public VmAllocationPolicyMigrationRoundRobinSocial(
        final VmSelectionPolicy vmSelectionPolicy,
        final double overUtilizationThreshold)
    {
        this(vmSelectionPolicy, overUtilizationThreshold, null);
    }

    /**
     * Creates a new VmAllocationPolicy, changing the {@link Function} to select a Host for a Vm.
     * @param vmSelectionPolicy the policy that defines how VMs are selected for migration
     * @param overUtilizationThreshold the over utilization threshold
     * @param findHostForVmFunction a {@link Function} to select a Host for a given Vm.
     *                              Passing null makes the Function to be set as the default {@link #findHostForVm(Vm)}.
     * @see VmAllocationPolicy#setFindHostForVmFunction(BiFunction)
     */
    public VmAllocationPolicyMigrationRoundRobinSocial(
        final VmSelectionPolicy vmSelectionPolicy,
        final double overUtilizationThreshold,
        final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction)
    {
        super(vmSelectionPolicy, overUtilizationThreshold, findHostForVmFunction);
    }

    @Override
    protected Optional<Host> findHostForVmInternal(final Vm vm, final Predicate<Host> predicate) {
        /*It's ignoring the super class intentionally to avoid the additional filtering performed there
         * and to apply a different method to select the Host to place the VM.*/
        var hostStream = getHostList().stream();
        if(((VmSocial)vm).owner == null)
        {
            return hostStream.filter(predicate).min(Comparator.comparingInt(host -> host.getVmList().size()));
            //return hostStream.max(Comparator.comparingDouble(Host::getCpuMipsUtilization));
        }
        else{
            return hostStream
                .filter(predicate)
                .filter( host -> ((VmSocial)vm).verifyHostSecurity((HostSocial) host))
                .max(Comparator.comparingDouble(Host::getCpuMipsUtilization));
        }

    }

}
