package ru.mjkey.linkedtrapdoors;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

// Система синхронизации соседних люков
public class TrapdoorEventSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final int MAX_LINKED = 64;
    private static final Set<Vector3i> currentlySyncing = Collections.synchronizedSet(new HashSet<>());
    
    private static final Vector3i[] NEIGHBORS = {
        Vector3i.POS_X, Vector3i.NEG_X,
        Vector3i.POS_Y, Vector3i.NEG_Y,
        Vector3i.POS_Z, Vector3i.NEG_Z
    };

    public TrapdoorEventSystem() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public void handle(final int index,
                       @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull final Store<EntityStore> store,
                       @Nonnull final CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull final UseBlockEvent.Pre event) {
        
        BlockType blockType = event.getBlockType();
        if (blockType == null) return;
        
        String blockId = blockType.getId();
        if (blockId == null || !blockId.toLowerCase().contains("trapdoor")) return;
        
        Vector3i targetPos = event.getTargetBlock();
        if (targetPos == null) return;
        
        if (currentlySyncing.contains(targetPos)) return;
        
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        
        World world = player.getWorld();
        if (world == null) return;
        
        // Если игрок в присядке - не синхронизируем
        if (isCrouching(player, world)) return;
        
        // Определяем целевое состояние (противоположное текущему)
        String targetState;
        if (blockId.contains("OpenDoor")) {
            targetState = "CloseDoorOut";
        } else if (blockId.contains("CloseDoor")) {
            targetState = "OpenDoorOut";
        } else {
            targetState = "OpenDoorOut";
        }
        
        syncAdjacentTrapdoors(world, targetPos, targetState);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    // Синхронизирует все соседние люки
    private void syncAdjacentTrapdoors(World world, Vector3i sourcePos, String targetState) {
        Set<Vector3i> linkedTrapdoors = findLinkedTrapdoors(world, sourcePos);
        currentlySyncing.addAll(linkedTrapdoors);
        
        try {
            for (Vector3i pos : linkedTrapdoors) {
                if (pos.equals(sourcePos)) continue;
                
                try {
                    BlockType neighborType = world.getBlockType(pos);
                    if (neighborType != null && isTrapdoor(neighborType)) {
                        world.setBlockInteractionState(pos, neighborType, targetState);
                    }
                } catch (Exception ignored) {}
            }
        } finally {
            currentlySyncing.removeAll(linkedTrapdoors);
        }
    }

    // Поиск всех связанных люков (BFS)
    private Set<Vector3i> findLinkedTrapdoors(World world, Vector3i start) {
        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();
        
        queue.add(start);
        visited.add(start);
        
        while (!queue.isEmpty() && visited.size() < MAX_LINKED) {
            Vector3i current = queue.poll();
            
            for (Vector3i offset : NEIGHBORS) {
                Vector3i neighbor = new Vector3i(
                    current.x + offset.x,
                    current.y + offset.y,
                    current.z + offset.z
                );
                
                if (visited.contains(neighbor)) continue;
                
                try {
                    BlockType neighborType = world.getBlockType(neighbor);
                    if (neighborType != null && isTrapdoor(neighborType)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                } catch (Exception ignored) {}
            }
        }
        
        return visited;
    }

    private boolean isTrapdoor(BlockType blockType) {
        if (blockType == null) return false;
        String id = blockType.getId();
        return id != null && id.toLowerCase().contains("trapdoor");
    }
    
    private boolean isCrouching(Player player, World world) {
        try {
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null) return false;
            
            MovementStatesComponent movementStates = world.getEntityStore().getStore()
                .getComponent(playerRef, MovementStatesComponent.getComponentType());
            if (movementStates == null) return false;
            
            return movementStates.getMovementStates().crouching;
        } catch (Exception e) {
            return false;
        }
    }
}
