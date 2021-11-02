package org.cloudbus.cloudsim.allocationpolicies.migration;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSocial;
import org.cloudbus.cloudsim.selectionpolicies.VmSelectionPolicy;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSocial;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Comparator.comparingDouble;

public class VmAllocationPolicyMigrationSocialCredit extends VmAllocationPolicyMigrationBestFitStaticThreshold{
    /**
     * Creates a VmAllocationPolicyMigrationBestFitStaticThreshold.
     * It uses a {@link #DEF_OVER_UTILIZATION_THRESHOLD default over utilization threshold}
     * and a {@link #DEF_UNDERLOAD_THRESHOLD default under utilization threshold}.
     *
     * @param vmSelectionPolicy the policy that defines how VMs are selected for migration
     * @see #setUnderUtilizationThreshold(double)
     * @see #setOverUtilizationThreshold(double)
     */
    public int goodBadThreshold;
    private double underUtilizationThreshold;

    public VmAllocationPolicyMigrationSocialCredit(final VmSelectionPolicy vmSelectionPolicy) {
        this(vmSelectionPolicy, DEF_OVER_UTILIZATION_THRESHOLD);
    }

    public VmAllocationPolicyMigrationSocialCredit(
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
    public VmAllocationPolicyMigrationSocialCredit(
        final VmSelectionPolicy vmSelectionPolicy,
        final double overUtilizationThreshold,
        final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction)
    {
        super(vmSelectionPolicy, overUtilizationThreshold, findHostForVmFunction);
    }

    /**
     * Sets the static host CPU utilization threshold to detect under
     * utilization.
     *
     * @param underUtilizationThreshold the underUtilizationThreshold to set
     */
    public final void setUnderUtilizationThreshold(final double underUtilizationThreshold) {
        if(underUtilizationThreshold <= 0 || underUtilizationThreshold >= 1){
            throw new IllegalArgumentException("Over utilization threshold must be greater than 0 and lower than 1.");
        }
        this.underUtilizationThreshold = underUtilizationThreshold;
    }

    @Override
    public double getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    /**
     * Checks if all VMs of a Host are <b>NOT</b> migrating out.
     * In this case, the given Host will not be selected as an underloaded Host at the current moment.
     * That is: not all VMs are migrating out if at least one VM isn't in migration process.
     *
     * @param host the host to check
     * @return true if at least one VM isn't migrating, false if all VMs are migrating
     */
    private boolean notAllVmsAreMigratingOut(final Host host) {
        return host.getVmList().stream().anyMatch(vm -> !vm.isInMigration());
    }


    /**
     * Gets the underloaded Host with the lowest social credit.
     * If a Host is underloaded but it has VMs migrating in,
     * then it's not included in the returned List
     * because the VMs to be migrated to move the Host from
     * the underload state already are in migration to it.
     * Likewise, if all VMs are migrating out, nothing has to be
     * done anymore. It just has to wait the VMs to finish
     * the migration.
     *
     * @param excludedHosts the Hosts that have to be ignored when looking for the under utilized Host
     * @return the most under utilized host or {@link Host#NULL} if no Host is found
     */
    private Host getUnderloadedHost(final Set<? extends Host> excludedHosts) {
        return this.getHostList().stream()
            .filter(host -> !excludedHosts.contains(host))
            .filter(Host::isActive)
            .filter(this::isHostUnderloaded)
            .filter(host -> host.getVmsMigratingIn().isEmpty())
            .filter(this::notAllVmsAreMigratingOut)
            .min(comparingDouble(Host::getCpuPercentUtilization))
            .orElse(Host.NULL);
    }


    @Override
    protected Optional<Host> findHostForVmInternal(final Vm vm, final Predicate<Host> predicate) {
        /*It's ignoring the super class intentionally to avoid the additional filtering performed there
         * and to apply a different method to select the Host to place the VM.*/
        var hostStream = getHostList().stream();
        if(((VmSocial)vm).owner == null)
        {
            return hostStream
                .filter(predicate)
                .max(Comparator.comparingDouble(Host::getCpuMipsUtilization));
        }
        else{
            return hostStream
                .filter(predicate)
                .filter( host -> ((VmSocial)vm).verifyHostSecurity((HostSocial) host))
                .min(Comparator.comparingInt(host -> ((HostSocial)host).getOwnerSocialCredit()));
        }
    }

}
