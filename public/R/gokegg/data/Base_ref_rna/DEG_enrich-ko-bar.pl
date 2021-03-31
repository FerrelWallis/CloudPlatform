#!usr/bin/perl -w

use strict;

die "usage:perl $0 <KO.enrich.xls> <Ko.enrich.bar.pdf> <num> \n" unless @ARGV == 3;
open A,$ARGV[0];
my $num=0;
while(<A>){
	chomp;my @a=split;
	next if(/^#/);
	if(/KEGG PATHWAY/){
		$num++;
	}
}
close A;
my $abc=$ARGV[2];
if($num < $ARGV[2]){
	$abc=$num;
}
open O,">cmd.r";
print O "par(mar=c(5,2,1,2)+0.1)
pdf(\"$ARGV[1]\",width=12,height=10)
dat <-read.delim(file=\"$ARGV[0]\",sep=\"\\t\",head=T,skip=4)
part<-dat[1:$abc,]
pvalue<--log(as.numeric(as.character(part[,6])),base=10)
ylim1<-floor(max(pvalue))+1
ylim0<-2*ylim1/5
xa <- barplot(pvalue,beside=TRUE,space=0.1,col=\"#4169E1\",axes=FALSE,ylim=c(-ylim0,ylim1),border=NA,plot=FALSE)
xlim1 <-max(xa)*1.02
xlim0 <--xlim1*0.15
xa <- barplot(pvalue,beside=TRUE,space=0.1,col=\"#4169E1\",axes=FALSE,ylim=c(-ylim0,ylim1),xlim=c(xlim0,xlim1*1.1),border=NA)
text(xa,y=-.1,srt=50,adj=1,labels=part[,1],xpd=T,cex=0.5)
axis(side=2, at=0:ylim1,las=1,cex=0.8,pos=c(0,0))
axis(side=2, at=ylim1/2,labels =\"-log10(Pvalue)\" ,las=3,cex.axis=0.9,pos=c(0,0),mgp=c(10,4,0),font.axis=3)
dev.off()\n";
close O;
`R --no-save < cmd.r`;
`rm cmd.r`;
