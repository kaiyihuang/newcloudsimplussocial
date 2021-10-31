package org.cloudbus.cloudsim.user;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class User {
    public String username;
    public int id;
    public ArrayList<User> friends;
    public HashMap<User, Integer> adjacency_map;
    public Host myDevice;
    public static int trust_threshold = 1;

    public int social_credit;
    public int trust_value;

    public int cloudlet_count;
    public int cloudlets_desired;

    public User(String username_in, int id_in) {
        username = username_in;
        id = id_in;
        friends = new ArrayList<User>();
    }

    public void make_friend(User friend)
    {
        if(!this.friends.contains(friend)) this.friends.add(friend);
        if(!friend.friends.contains(this)) friend.friends.add(this);
    }

    public void update_network(ArrayList<User> full_user_list)
    {
        this.adjacency_map = this.dijkstra_costs(full_user_list);
    }

    public void print_adjacency_list()
    {
        for(User user: this.adjacency_map.keySet()) {
            String key = user.username;
            String distance = adjacency_map.get(user).toString();
            System.out.println(key + ": " + distance);
        }
    }

    public void bind_host(Host new_device)
    {
        myDevice = new_device;
    }


    private int getShortestDistance(ArrayList<User> masterlist, User dest, User source) {
        int v = masterlist.size();
        int pred[] = new int[v];
        int dist[] = new int[v];
        if(dest == source) {
            return 0;
        }
        if(BFS(masterlist, dest, source, pred, dist) == false) {
            System.out.println("Given source and destination" +
                "are not connected");
            return -1;
        }

        LinkedList<User> path = new LinkedList<User>();
        User crawl = dest;
        path.add(crawl);
        while (pred[masterlist.indexOf(crawl)] != -1) {
            path.add(masterlist.get(pred[masterlist.indexOf(crawl)]));
            crawl = masterlist.get(pred[masterlist.indexOf(crawl)]);
        }

        return dist[masterlist.indexOf(dest)];

    }

    private boolean BFS(ArrayList<User> masterlist, User dest, User source, int pred[], int dist[])
    {
        int v = masterlist.size();
        LinkedList<User> queue = new LinkedList<User>();
        boolean visited[] = new boolean[v];
        int src = masterlist.indexOf(source);

        for (int i=0;i<v; i++) {
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
            pred[i] = -1;
        }

        visited[src] = true;
        dist[src] = 0;
        queue.add(source);
        while(!queue.isEmpty()) {
            User u = queue.remove();
            for(int i=0;i<u.friends.size();i++)
            {
                if (visited[masterlist.indexOf(u.friends.get(i))] == false)
                {
                    visited[masterlist.indexOf(u.friends.get(i))] = true;
                    dist[masterlist.indexOf(u.friends.get(i))] = dist[masterlist.indexOf(u)] + 1;
                    pred[masterlist.indexOf(u.friends.get(i))] = masterlist.indexOf(u);
                    queue.add(u.friends.get(i));

                    //stopping condition
                    if (u.friends.get(i) == dest) return true;
                }
            }
        }
        return false;


    }

    private HashMap dijkstra_costs(ArrayList<User> full_user_list)
    {
        HashMap<User, Integer> adjacency_map_temp = new HashMap<User, Integer>();
        int dist;

        for(User u: full_user_list)
        {
            dist = getShortestDistance(full_user_list,u,this);
            adjacency_map_temp.put(u, dist);
        }
        return adjacency_map_temp;
    }

}
