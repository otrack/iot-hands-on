package eu.tsp.distsum;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Test
public class DistributedSum extends MultipleCacheManagersTest{

    // the number of slaves
    private static final int NUMBER_SLAVES = 1;

    // how many rounds of computation do we execute
    private static final int NUMBER_ROUNDS = 3;

    public void distributedSumTest() {

        // create the communication channel between the master and the slaves
        Channel channel = new Channel(manager(0).getCache());
        Master coord = new Master(channel);

        // create the initial values and constraints for the slaves
        ArrayList<Slave> slaves = new ArrayList<>(NUMBER_SLAVES);
        Map<String, Integer> slaveValues = new HashMap<>(NUMBER_SLAVES);
        Map<String, Constraint> slaveConstraints = new HashMap<>(NUMBER_SLAVES);
        int initValue = 10;
        Constraint initConstraint = new Constraint(9, 11);
        for (int s = 1; s <= NUMBER_SLAVES; s++) {
            // create a new slave
            channel = new Channel(manager(s).getCache());
            Slave slave = new Slave(Integer.toString(s), initValue, initConstraint, channel);
            slaves.add(slave);
            slaveValues.put(slave.getId(), slave.getLocalValue());
            slaveConstraints.put(slave.getId(), slave.getConstraint());
            // register slave to the channel
            channel.register(slave.getId(), slave);
        }

        // initialize the structures kept by the master
        coord.setLocalValues(slaveValues);
        coord.setConstraints(slaveConstraints);

        // run the test
        int[] updates = { 1, 1, 1, 1, 2, 2, -1, -1, -2 };
        Random rand = new Random();
        for (int round = 0; round < NUMBER_ROUNDS; round++) {
            System.out.println("********* ROUND " + round + " ********");
            int realSum = 0;
            for (int slave = 0; slave < NUMBER_SLAVES; slave++) {
                int update = updates[rand.nextInt(updates.length)];
                int oldValue = slaves.get(slave).getLocalValue();
                slaves.get(slave).update(update);

                System.out.println(
                        "slave " + slave
                                + ": update=" + update
                                + " oldValue=" + oldValue
                                + " newValue=" + slaves.get(slave).getLocalValue());
                realSum += slaves.get(slave).getLocalValue();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Real sum: " + realSum + " Global Sum: " + coord.getGlobalSum());
            System.out.println("********* END OF ROUND " + round + " ********\n\n");
            assert (0.5 * realSum <= coord.getGlobalSum());
            assert (1.5 * realSum >= coord.getGlobalSum());
        }

    }

    @Override
    protected void createCacheManagers() throws Throwable {
        super.createCluster(getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC), NUMBER_SLAVES+1);
    }
}

