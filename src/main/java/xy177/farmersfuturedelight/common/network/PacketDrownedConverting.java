package xy177.farmersfuturedelight.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xy177.farmersfuturedelight.FarmerFutureDelight;

public final class PacketDrownedConverting implements IMessage {
    private int dimension;
    private int entityId;
    private boolean converting;

    public PacketDrownedConverting() {
    }

    public PacketDrownedConverting(EntityLivingBase entity, boolean converting) {
        dimension = entity.dimension;
        entityId = entity.getEntityId();
        this.converting = converting;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        entityId = buffer.readInt();
        converting = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeInt(entityId);
        buffer.writeBoolean(converting);
    }

    public static final class Handler implements IMessageHandler<PacketDrownedConverting, IMessage> {
        @Override
        public IMessage onMessage(PacketDrownedConverting message, MessageContext context) {
            FarmerFutureDelight.proxy.handleDrownedConverting(
                    message.dimension, message.entityId, message.converting);
            return null;
        }
    }
}
