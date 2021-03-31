#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_exp.diff> <diff type 0 :fdr<=0.05 1:p=0.05 2:p=0.01 3:fdr <=0.05 & one FPKM >= 5> <anno.info> \n" unless @ARGV == 2 || @ARGV == 3;

open A,$ARGV[0]; 	my %h;	my %gene;
open OUT,">$ARGV[0].data"; 
if(defined $ARGV[2]){
	open B,$ARGV[2];
	while(<B>){
		chomp;my @a=split /\t+/;		my $inf=join "\t",@a[1..$#a];		$h{$a[0]}=$inf;
	}
	close B;
}
print "gene_id\tlocus\tsample_1\tsample_2\tfpkm_1\tfpkm_2\tlog2(sample_2/sample_1)\tp_value\tq_value\ttype\tfunc_anno\n";
my %id_sign;
my $head=<A>;   chomp $head;    my @h=split /\t+/,$head;
print OUT "Rec\t$h[0]\t$h[4]\t$h[5]\t$h[7]\t$h[8]\t$h[9]\t$h[11]\t$h[12]\tsign\n";        my $rec=0;
while(<A>){
	chomp;my @a=split /\t+/;	my $fd=$a[9];	my $sign=0;	$rec++;	
	if($a[9]=~/inf/){
		if($a[7] < $a[8]){                      $fd=10;                }
		else{                                   $fd=-10;               }
	}
	$id_sign{$.}=[$fd,$sign,$rec];	
	if($ARGV[1] == 0){
		next if($a[-2] > 0.05);	next if($fd <= 1 && $fd >= -1);
		if($a[7] < $a[8]){                            $sign=1;                        }
		else{                                         $sign=2;                        }
	}
	if($ARGV[1] == 1){
		next if($fd <= 1 && $fd >= -1);		next if($a[11] >= 0.05);
		if(($a[9] >= 1 || $a[9] <= -1) && $a[11] <= 0.05){
			if($a[7] < $a[8]){                              $sign=1;                        }
			else{                           $sign=2;                        }
		}
	}
	if($ARGV[1] == 2){
		next if($fd <= 1 && $fd >= -1);		next if($a[11] >= 0.01);
		if($a[7] < $a[8]){                              $sign=1;                        }
		else{                                           $sign=2;                        }
	}
	my $type="down";
	if($ARGV[1] == 3){
		next if($a[7] < 5 && $a[8] < 5);
		next if($a[-2] >= 0.05);  next if($fd <= 1 && $fd >= -1);
		if($a[7] < $a[8]){              $sign=1;            }
		else{                           $sign=2;            }
	}
	if($a[8] > $a[7]){		$type="up";	}
	if(exists $h{$a[0]}){		print "$a[0]\t$a[3]\t$a[4]\t$a[5]\t$a[7]\t$a[8]\t$a[9]\t$a[11]\t$a[12]\t$type\t$h{$a[0]}\n";	}
	else{						print "$a[0]\t$a[3]\t$a[4]\t$a[5]\t$a[7]\t$a[8]\t$a[9]\t$a[11]\t$a[12]\t$type\t-\n";			}
	$id_sign{$.}=[$fd,$sign,$rec];	
}
close A;
open A,$ARGV[0];	<A>;
while(<A>){
	chomp;my @a=split /\t+/;
	print OUT "$id_sign{$.}[2]\t$a[0]\t$a[4]\t$a[5]\t$a[7]\t$a[8]\t$id_sign{$.}[0]\t$a[11]\t$a[12]\t$id_sign{$.}[1]\n";	
}
close A;close OUT;
open RCMD, ">$ARGV[0].cmd.r";
print RCMD "diff<-read.delim(\"$ARGV[0].data\",check.names=F,header=T,sep=\"\\t\")
pdf(\"$ARGV[0].data.pdf\",w=18,h=8);
FPKM1<-diff[,5]
FPKM2<-diff[,6]
n1<-diff[1,3]
n2<-diff[1,4]
fdr<-diff[,8];
logFold<-diff[,7];
upL<-diff[which(diff[,10]==1),1]
downL<-diff[which(diff[,10]==2),1]
gridlayout = matrix(c(1:2),nrow=1,ncol=2, byrow=TRUE)
layout(gridlayout)

par(mar=c(6,6,6,1))
pchlist<-rep(1,length(FPKM1))
pchlist[c(upL,downL)]<-rep(16,length(c(upL,downL)))
collist<-rep(rgb(34,30,31,max=255,alpha=200),length(FPKM1))
collist[upL]<-rep(rgb(238,79,43,max=255,alpha=200),length(upL))
collist[downL]<-rep(rgb(44,96,173,max=255,alpha=200),length(downL))
nochange<-length(FPKM1)-length(upL)-length(downL)
corr<-round(cor(FPKM1,FPKM2,method=\"pearson\"),4)
legend<-c(paste(\" pearson correlation: \",corr,sep=\"\"),paste(\"up-regulated genes (\",length(upL),\")\",sep=\"\"),paste(\"down-regulated genes (\",length(downL),\")\",sep=\"\"),paste(\"not differential expressed (\",nochange,\")\",sep=\"\"))
plot(FPKM1,FPKM2,log='xy',pch=pchlist,col=collist,xlab=paste(\"FPKM of \",n1,sep=\"\"),ylab=paste(\"FPKM of \",n2,sep=\"\"),main=\"Scatter plot\")
legend(\"topleft\",pch=16,legend=legend,inset=0.015,col=c(\"white\",rgb(238,79,43,max=255),rgb(44,96,173,max=255),rgb(34,30,31,max=255)),cex=1,bty=\"n\")

par(mar=c(6,6,6,6))
pchlist<-rep(1,length(logFold))
pchlist[c(upL,downL)]<-rep(16,length(c(upL,downL)))
collist<-rep(rgb(34,30,31,max=255,alpha=200),length(logFold))
collist[upL]<-rep(rgb(238,79,43,max=255,alpha=200),length(upL))
collist[downL]<-rep(rgb(44,96,173,max=255,alpha=200),length(downL))
nochange<-length(logFold)-length(upL)-length(downL)
legend<-c(paste(\"up-regulated genes (\",length(upL),\")\",sep=\"\"),paste(\"down-regulated genes(\",length(downL),\")\",sep=\"\"),paste(\"not differential expressed(\",nochange,\")\",sep=\"\"))
plot(logFold, -1*log10(fdr), pch=pchlist,col=collist,xlab=paste(\"Log(FC)\"),ylab=paste(\"-log10(pavlue)\",sep=\"\"), main=\"Volcano plot\")
legend(\"topleft\",pch=16,bty=\"n\",legend=legend,inset=0.015,col=c(rgb(238,79,43,max=255),rgb(44,96,173,max=255),rgb(34,30,31,max=255)),cex=1)
dev.off()\n";
`R --no-save < $ARGV[0].cmd.r`;
#`rm $ARGV[0].cmd.r`;
