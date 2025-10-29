package net.shiroha233.roadweaver.persistence.attachments;

import com.mojang.serialization.Codec;
import net.shiroha233.roadweaver.RoadWeaver;
import net.shiroha233.roadweaver.helpers.Records;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class WorldDataAttachment {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadWeaver.MOD_ID);

    public static final AttachmentType<List<Records.StructureConnection>> CONNECTED_STRUCTURES = AttachmentRegistry.createPersistent(
            new ResourceLocation(RoadWeaver.MOD_ID, "connected_villages"),
            Codec.list(Records.StructureConnection.CODEC)
    );


    public static final AttachmentType<Records.StructureLocationData> STRUCTURE_LOCATIONS = AttachmentRegistry.createPersistent(
            new ResourceLocation(RoadWeaver.MOD_ID, "village_locations"),
            Records.StructureLocationData.CODEC
    );

    public static final AttachmentType<List<Records.RoadData>> ROAD_DATA_LIST = AttachmentRegistry.createPersistent(
            new ResourceLocation(RoadWeaver.MOD_ID, "road_chunk_data_map"),
            Codec.list(Records.RoadData.CODEC)
    );


    public static void registerWorldDataAttachment() {
        LOGGER.info("Registering WorldData attachment");
    }
}
