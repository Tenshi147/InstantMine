package me.kami2q1.opfern.module.modules.exploit;

import me.kami2q1.opfern.command.Command;
import me.kami2q1.opfern.event.events.*;
import me.kami2q1.opfern.module.Module;
import me.kami2q1.opfern.module.Module.Category;
import me.kami2q1.opfern.module.modules.combat.CrystalAura;
import me.kami2q1.opfern.setting.Setting;
import me.kami2q1.opfern.setting.Settings;
import me.kami2q1.opfern.util.GeometryMasks;
import me.kami2q1.opfern.util.OpfernTessellator;
import me.kami2q1.opfern.util.Timer;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;

@Module.Info(name = "InstantMine", category = Category.EXPLOIT)
public class SecretTest extends Module {
	
	private BlockPos renderBlock;
	private BlockPos lastBlock;
	private boolean packetCancel = false;
	private Timer breaktimer = new Timer();
	private EnumFacing direction;

	private Setting<Boolean> autoBreak = register(Settings.booleanBuilder("Auto Break").withValue(true).build());
	private Setting<Integer> delay = register(Settings.integerBuilder("Delay").withValue(20).withMinimum(0).withMaximum(500).build());
	private Setting<Boolean> picOnly = register(Settings.booleanBuilder("Only Pickaxe").withValue(true).build());

	public static SecretTest INSTANCE;

	@Override
	protected void onEnable() {
		INSTANCE = this;
	}

	public static SecretTest getInstance(){
		if(INSTANCE==null){
			INSTANCE = new SecretTest();
		}
		return INSTANCE;
	}

	@Override
	public void onWorldRender(RenderEvent event) {
		if (renderBlock != null) {
			drawBlock(renderBlock, 255, 0, 255, true);
		}
	}

	private void drawBlock(BlockPos blockPos, int r, int g, int b, boolean bounding) {
		Color color = new Color(r, g, b, 40);
		OpfernTessellator.prepare(GL11.GL_QUADS);
		OpfernTessellator.drawBox(blockPos, color.getRGB(), GeometryMasks.Quad.ALL);
		OpfernTessellator.release();
	}

	@Override
	public void onUpdate() {
		if(renderBlock != null) {
			if(autoBreak.getValue() && breaktimer.passed(delay.getValue())) {
				if(picOnly.getValue()&&!(mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.DIAMOND_PICKAXE)) return;
				mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
						renderBlock, direction));
				breaktimer.reset();
			}

		}
		
		try {
			mc.playerController.blockHitDelay = 0;

		} catch (Exception e) {
		}
	}

	@EventHandler
	private Listener<PacketEvent.Send> packetSendListener = new Listener<>(event -> {
		Packet packet = event.getPacket();
		if (packet instanceof CPacketPlayerDigging) {
			CPacketPlayerDigging digPacket = (CPacketPlayerDigging) packet;
			if(((CPacketPlayerDigging) packet).getAction()== CPacketPlayerDigging.Action.START_DESTROY_BLOCK && packetCancel) event.cancel();
		}
	});

	@EventHandler
	private Listener<DamageBlockEvent> OnDamageBlock = new Listener<>(p_Event -> {
		if (canBreak(p_Event.getPos())) {

			if(lastBlock==null||p_Event.getPos().x!=lastBlock.x || p_Event.getPos().y!=lastBlock.y || p_Event.getPos().z!=lastBlock.z) {
				//Command.sendChatMessage("New Block");
				packetCancel = false;
				//Command.sendChatMessage(p_Event.getPos()+" : "+lastBlock);
				mc.player.swingArm(EnumHand.MAIN_HAND);
				mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
						p_Event.getPos(), p_Event.getDirection()));
				packetCancel = true;
			}else{
				packetCancel = true;
			}
			//Command.sendChatMessage("Breaking");
			mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
					p_Event.getPos(), p_Event.getDirection()));

			renderBlock = p_Event.getPos();
			lastBlock = p_Event.getPos();
			direction = p_Event.getDirection();

			p_Event.cancel();

		}
	});

	private boolean canBreak(BlockPos pos) {
		final IBlockState blockState = mc.world.getBlockState(pos);
		final Block block = blockState.getBlock();

		return block.getBlockHardness(blockState, mc.world, pos) != -1;
	}

	public BlockPos getTarget(){
		return renderBlock;
	}

	public void setTarget(BlockPos pos){
		renderBlock = pos;
		packetCancel = false;
		mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
				pos, EnumFacing.DOWN));
		packetCancel = true;
		mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
				pos, EnumFacing.DOWN));
		direction = EnumFacing.DOWN;
		lastBlock = pos;
	}

}
