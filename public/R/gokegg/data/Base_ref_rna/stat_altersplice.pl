#!usr/bin/perl -w

use strict;

die "usage:perl $0 <alter splice list> \n" unless @ARGV == 1;

open A,$ARGV[0];
my %sam;	my %tar;
my @type=qw/A3SS A5SS MXE RI SE/;

while(<A>){
    chomp;my @a=split /\//;	my @b=split /\./,$a[-1];
    open IN,$_;	<IN>;	    $sam{$a[-2]}=1;
	my $num=0;
    while(my $inf=<IN>){
		$num++;
    }
    close IN;
	$tar{$b[0]}{$a[-2]}=$num;
}
close A;
my @sam=sort keys %sam;
my $head=join "\t",@sam;
print "type\t$head\n";
foreach my $k1(@type){
    print "$k1";
    foreach my $k2(@sam){
		if(exists $tar{$k1}{$k2}){
	        print "\t$tar{$k1}{$k2}";
		}
		else{
			print "\t0";
		}
    }
    print "\n";
}
