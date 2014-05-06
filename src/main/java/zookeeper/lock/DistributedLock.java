package zookeeper.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

public class DistributedLock extends ConnectionWatcher {

	public String join(String groupPath) throws KeeperException,
			InterruptedException {

		String path = groupPath + "/lock-" + zk.getSessionId() + "-";

		// ����һ��˳����ʱ�ڵ�
		String createdPath = zk.create(path, null/* data */,
				Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println("Created " + createdPath);
		return createdPath;
	}

	// ��鱾�ͻ����Ƿ�õ��˷ֲ�ʽ��
	public boolean checkState(String groupPath, String myName)
			throws KeeperException, InterruptedException {

		List<String> childList = zk.getChildren(groupPath, false);

		String[] myStr = myName.split("-");
		long myId = Long.parseLong(myStr[2]);

		boolean minId = true;
		for (String childName : childList) {
			String[] str = childName.split("-");
			long id = Long.parseLong(str[2]);
			if (id < myId) {
				minId = false;
				break;
			}
		}

		if (minId) {
			System.out.println(new Date() + "�ҵõ��˷ֲ����������� myId:" + myId);

			return true;
		} else {
			System.out.println(new Date() + "����Ŭ���ɣ�  myId:" + myId);

			return false;
		}
	}

	// �����ͻ���û�еõ��ֲ�ʽ��������м������ڵ�ǰ��Ľڵ�(������ȺЧӦ)
	public void listenNode(final String groupPath, final String myName)
			throws KeeperException, InterruptedException {

		List<String> childList = zk.getChildren(groupPath, false);

		String[] myStr = myName.split("-");
		long myId = Long.parseLong(myStr[2]);

		List<Long> idList = new ArrayList<Long>();
		Map<Long, String> sessionMap = new HashMap<Long, String>();

		for (String childName : childList) {
			String[] str = childName.split("-");
			long id = Long.parseLong(str[2]);
			idList.add(id);
			sessionMap.put(id, str[1] + "-" + str[2]);
		}

		Collections.sort(idList);

		int i = idList.indexOf(myId);
		if (i <= 0) {
			throw new IllegalArgumentException("���ݴ���");
		}

		// �õ�ǰ���һ���ڵ�
		long headId = idList.get(i - 1);

		String headPath = groupPath + "/lock-" + sessionMap.get(headId);
		System.out.println("��Ӽ�����" + headPath);

		Stat stat = zk.exists(headPath, new Watcher() {

			public void process(WatchedEvent event) {
				System.out.println("�Ѿ�������" + event.getType() + "�¼���");

				try {
					while (true) {
						if (checkState(groupPath, myName)) {
							Thread.sleep(3000);
							System.out.println(new Date() + " ϵͳ�رգ�");
							System.exit(0);
						}

						Thread.sleep(3000);

					}
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		});

		System.out.println(stat);

	}
	
	public static void main(String[] args) throws Exception {  
        DistributedLock joinGroup = new DistributedLock();  
        joinGroup.connect("10.25.22.19:" + "2181");  
      
        //zookeeper�ĸ��ڵ㣻���б�����ǰ����Ҫ��ǰ����  
        String groupName = "zkRoot";  
        String memberName = "_locknode_";  
        String path = "/" + groupName + "/" + memberName;  
          
        String myName = joinGroup.join(path);  
        if (!joinGroup.checkState(path, myName)) {  
            joinGroup.listenNode(path, myName);  
        }  
          
        Thread.sleep(Integer.MAX_VALUE);  
          
		joinGroup.close();  
    }  

}
