package xy177.farmersfuturedelight.common.command;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.logging.log4j.LogManager;
import xy177.farmersfuturedelight.common.FFDConfig;

public final class CommandCompatibility extends CommandBase {
    @Override
    public String getName() {
        return "ffd_compat";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ffd_compat preset";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 1 || !"preset".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }
        int count;
        try {
            count = FFDConfig.applyAgreedAutoCompatibilityPreset();
        } catch (IOException failure) {
            LogManager.getLogger("FFD Auto Compatibility").error(
                    "Could not save the agreed compatibility preset", failure);
            throw new CommandException("commands.farmers_future_delight.compat.save_failed");
        }
        sender.sendMessage(new TextComponentTranslation(
                "commands.farmers_future_delight.compat.applied", count));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "preset")
                : Collections.emptyList();
    }
}
