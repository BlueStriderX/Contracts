/**
 * Packet [Client -> Server]
 */
package thederpgamer.contracts.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.ItemStack;
import thederpgamer.contracts.Contracts;
import thederpgamer.contracts.contracts.Contract;
import thederpgamer.contracts.contracts.target.*;
import thederpgamer.contracts.util.DataUtils;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.PlayerNotFountException;

import java.io.IOException;

public class AddContractPacket extends Packet {

    private Contract contract;
    private String contractName;
    private String contractorID;
    private String contractType;
    private String contractReward;
    private String contractLocation;
    private String contractTarget;
    private String contractUID;
    private Contracts.Mode gameState = Contracts.getInstance().getGameState();

    public AddContractPacket() {

    }

    public AddContractPacket(Contract contract) {
        this.contract = contract;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        if(gameState.equals(Contracts.Mode.SERVER)) {
            contractName = packetReadBuffer.readString();
            contractorID = packetReadBuffer.readString();
            contractType = packetReadBuffer.readString();
            contractReward = packetReadBuffer.readString();
            contractLocation = packetReadBuffer.readString();
            contractTarget = packetReadBuffer.readString();
            contractUID = packetReadBuffer.readString();
        }
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        if(gameState.equals(Contracts.Mode.CLIENT)) {
            packetWriteBuffer.writeString(contractName);
            packetWriteBuffer.writeString(contractorID);
            packetWriteBuffer.writeString(contractType);
            packetWriteBuffer.writeString(contractReward);
            packetWriteBuffer.writeString(contractTarget);
            packetWriteBuffer.writeString(contractUID);
        }
    }

    @Override
    public void processPacketOnClient() {
        try {
            contractName = contract.getName();
            contractorID = String.valueOf(contract.getContractor().getID());
            contractType = contract.getContractType().toString();
            contractReward = String.valueOf(contract.getReward());
            if (contract.getContractType().equals(Contract.ContractType.CARGO_ESCORT)) {
                contractLocation = contract.getTarget().getLocation()[0] + "," + contract.getTarget().getLocation()[1] + "," + contract.getTarget().getLocation()[2];
                for (Object object : contract.getTarget().getTargets()) {
                    ItemStack itemStack = (ItemStack) object;
                    contractTarget = itemStack.getId() + "," + itemStack.getAmount() + ";";
                }
            } else {
                contractLocation = "null";
            }
            contractUID = contract.getUID();
        } catch (PlayerNotFountException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        ContractTarget target = null;
        if(Contract.ContractType.valueOf(contractType).equals(Contract.ContractType.BOUNTY)) {
            target = new PlayerTarget();
            target.setTargets(contractTarget);
        } else if(Contract.ContractType.valueOf(contractType).equals(Contract.ContractType.CARGO_ESCORT)) {
            target = new CargoTarget();
            target.setTargetsFromString(contractTarget);
            target.setLocationFromString(contractLocation);
        } else if(Contract.ContractType.valueOf(contractType).equals(Contract.ContractType.MINING)) {
            target = new MiningTarget();
            target.setTargetsFromString(contractTarget);
        } else if(Contract.ContractType.valueOf(contractType).equals(Contract.ContractType.PRODUCTION)) {
            target = new ProductionTarget();
            target.setTargetsFromString(contractTarget);
        }

        contract = new Contract(Integer.parseInt(contractorID), contractName, Contract.ContractType.valueOf(contractType), Integer.parseInt(contractReward), contractUID, target);
        DataUtils.addContract(contract);
    }
}
